from datetime import datetime, timezone
from typing import Any, Dict
import asyncio
import logging

from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import insert, select

from app.models import Interaction, Rating, UserFeatures, Movie

logger = logging.getLogger(__name__)


class EventService:
    def __init__(self, cache):
        self.cache = cache

    async def process_event(
        self,
        event_type: str,
        user_id: str,
        movie_id: str | None,
        metadata: Dict[str, Any] | None,
        client_timestamp: str | None,
        request_id: str | None,
        session: AsyncSession,
    ) -> Dict[str, Any]:
        logger.info("Processing event: type=%s, user=%s, movie=%s", event_type, user_id, movie_id)
        try:
            # Get recommendation engine for prediction
            from app.routes import recommendations
            engine = getattr(recommendations, "recommendation_engine", None)
            if engine is None:
                logger.warning("Recommendation engine not available for score prediction")

            # Validate movie exists when provided by calling movie service
            if movie_id:
                from app.config import get_settings
                import httpx

                settings = get_settings()
                movie_url = settings.movie_service_url.rstrip("/") + f"/api/v1/movies/{movie_id}"
                try:
                    async with httpx.AsyncClient(timeout=5.0) as client:
                        resp = await client.get(movie_url)
                        if resp.status_code == 404:
                            raise ValueError(f"movie not found: {movie_id}")
                        if resp.status_code >= 400:
                            raise ValueError(f"movie service error: {resp.status_code}")
                except httpx.RequestError as e:
                    raise ValueError(f"failed to validate movie id via movie service: {e}")

            # Persist interaction
            try:
                stmt = insert(Interaction).values(
                    event_type=event_type,
                    request_id=request_id,
                    user_id=user_id,
                    movie_id=movie_id,
                    client_timestamp=client_timestamp,
                    meta=metadata,
                )
                await session.execute(stmt)
                await session.commit()
            except Exception as e:
                logger.error("Failed to persist interaction: %s", e, exc_info=True)
                raise

            # If rating event, persist rating
            if event_type == "rating" and metadata:
                rating_val = metadata.get("rating")
                if rating_val is not None:
                    try:
                        rstmt = insert(Rating).values(
                            user_id=user_id,
                            movie_id=movie_id,
                            rating=rating_val
                        )
                        await session.execute(rstmt)
                        await session.commit()
                    except Exception as e:
                        logger.error("Failed to persist rating: %s", e, exc_info=True)
                        raise

            # Update user features (cache disabled)
            try:
                # try DB
                q = select(UserFeatures).where(UserFeatures.user_id == user_id)
                res = await session.execute(q)
                row = res.scalar_one_or_none()
                if row:
                    features = row.features
                    logger.info("Loaded existing features for user %s: ratings keys=%d", user_id, len(features.get("ratings", {})))
                else:
                    features = {
                        "totalInteractions": 0,
                        "watchHistory": [],
                        "favorites": [],
                        "ratings": {}
                    }
                    logger.info("Created new features for user %s", user_id)

                features["totalInteractions"] = features.get("totalInteractions", 0) + 1
                if event_type == "watch" and movie_id:
                    features.setdefault("watchHistory", []).append({
                        "movieId": movie_id,
                        "ts": datetime.now(timezone.utc).isoformat(),
                        "metadata": metadata,
                    })

                if event_type == "favorite" and metadata:
                    action = metadata.get("action")
                    if action == "add":
                        if movie_id and movie_id not in features.get("favorites", []):
                            features.setdefault("favorites", []).append(movie_id)
                    elif action == "remove":
                        try:
                            features.setdefault("favorites", []).remove(movie_id)
                        except Exception:
                            pass

                # Update ratings based on event type
                if movie_id:
                    current_score = features.setdefault("ratings", {}).get(movie_id, 0.0)
                    predicted = 0.0
                    try:
                        if engine:
                            predicted = await engine.predict_rating(user_id, movie_id)
                    except Exception as e:
                        logger.warning("Failed to get prediction for user %s, movie %s: %s", user_id, movie_id, e)
                        predicted = 0.0
                    
                    if event_type == "rating" and metadata:
                        rating_val = metadata.get("rating")
                        if rating_val is not None:
                            # Combine user rating with model prediction
                            combined_score = 0.7 * rating_val + 0.3 * predicted
                            features["ratings"][movie_id] = combined_score
                            logger.info("Rating update: user=%s, movie=%s, user_rating=%.2f, predicted=%.2f, combined=%.2f", user_id, movie_id, rating_val, predicted, combined_score)
                    elif event_type == "watch":
                        # Increase score slightly for watch
                        new_score = min(current_score + 0.1, 5.0)  # Cap at 5.0
                        combined_score = 0.7 * new_score + 0.3 * predicted
                        features["ratings"][movie_id] = combined_score
                        logger.info("Watch update: user=%s, movie=%s, old_score=%.2f, new_score=%.2f, predicted=%.2f, combined=%.2f", user_id, movie_id, current_score, new_score, predicted, combined_score)
                    elif event_type == "favorite" and metadata:
                        action = metadata.get("action")
                        if action == "add":
                            # Set high score for favorite
                            new_score = 4.5
                            combined_score = 0.7 * new_score + 0.3 * predicted
                            features["ratings"][movie_id] = combined_score
                            logger.info("Favorite add: user=%s, movie=%s, set_score=%.2f, predicted=%.2f, combined=%.2f", user_id, movie_id, new_score, predicted, combined_score)
                        elif action == "remove":
                            # Decrease score for unfavorite
                            new_score = max(current_score - 0.5, 0.0)
                            combined_score = 0.7 * new_score + 0.3 * predicted
                            features["ratings"][movie_id] = combined_score
                            logger.info("Favorite remove: user=%s, movie=%s, old_score=%.2f, new_score=%.2f, predicted=%.2f, combined=%.2f", user_id, movie_id, current_score, new_score, predicted, combined_score)
                    elif event_type == "search":
                        # Slight increase for search
                        new_score = min(current_score + 0.05, 5.0)
                        combined_score = 0.7 * new_score + 0.3 * predicted
                        features["ratings"][movie_id] = combined_score
                        logger.info("Search update: user=%s, movie=%s, old_score=%.2f, new_score=%.2f, predicted=%.2f, combined=%.2f", user_id, movie_id, current_score, new_score, predicted, combined_score)

                logger.info("Features after update for user %s: ratings=%s", user_id, features.get("ratings", {}))

                # persist features to cache and DB (cache disabled)
                # await self.cache.set_user_features(user_id, features)
            except Exception as e:
                logger.error("Failed to update user features: %s", e, exc_info=True)
                raise

            # Invalidate recommendations cache for this user and schedule an immediate refresh
            try:
                await self.cache.invalidate_recommendations(user_id)
            except Exception:
                logger.debug("Failed to invalidate recommendations cache for user %s", user_id, exc_info=True)

            try:
                from app.routes import recommendations

                engine = getattr(recommendations, "recommendation_engine", None)

                if engine:
                    async def _refresh_recs():
                        try:
                            recs = await engine.get_recommendations(user_id=user_id, features=features, k=20, context="home")
                            await self.cache.set_recommendations(user_id, recs)
                        except Exception:
                            logger.exception("Failed to refresh recommendations for user %s", user_id)

                    # Schedule refresh in background so event processing isn't blocked by recommender errors
                    try:
                        asyncio.create_task(_refresh_recs())
                    except Exception:
                        logger.exception("Failed to schedule recommendation refresh for user %s", user_id)
            except Exception:
                logger.debug("Recommendation engine not available for immediate refresh", exc_info=True)

            # upsert into user_features table
            try:
                q = select(UserFeatures).where(UserFeatures.user_id == user_id)
                res = await session.execute(q)
                row = res.scalar_one_or_none()
                if row:
                    row.features = features
                    session.add(row)
                    await session.commit()
                    logger.info("Updated user_features for user %s in DB", user_id)
                else:
                    uf = UserFeatures(user_id=user_id, features=features)
                    session.add(uf)
                    await session.commit()
                    logger.info("Inserted new user_features for user %s in DB", user_id)
            except Exception as e:
                logger.error("Failed to upsert user features to DB: %s", e, exc_info=True)
                raise

            # Kafka publishing disabled — using DB/cache only

            return {
                "requestId": request_id,
                "status": "ok",
                "serverTimestamp": datetime.now(timezone.utc).isoformat()
            }
        except Exception as e:
            logger.error("Event processing failed for user %s, event %s: %s", user_id, event_type, e, exc_info=True)
            raise
