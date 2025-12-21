"""
API Routes for recommendations
"""
from datetime import datetime, timezone
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from fastapi.responses import JSONResponse
from sqlalchemy.ext.asyncio import AsyncSession
import asyncio

from app.database import get_db
from app.cache import cache
from app.recommendation_engine import RecommendationEngine
from app.feature_extractor import FeatureExtractor
from app.schemas import (
    RecommendationResponse,
    RecommendationItem,
    UserFeaturesResponse
)
from app.config import get_settings
from app import model_trainer
from app.cache import cache as global_cache
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1", tags=["Recommendations"])

settings = get_settings()

# Initialize engines (will be properly initialized in main.py)
recommendation_engine: Optional[RecommendationEngine] = None
feature_extractor: Optional[FeatureExtractor] = None


def get_recommendation_engine() -> RecommendationEngine:
    """Get recommendation engine instance"""
    global recommendation_engine
    if recommendation_engine is None:
        recommendation_engine = RecommendationEngine(cache)
    return recommendation_engine


def get_feature_extractor() -> FeatureExtractor:
    """Get feature extractor instance"""
    global feature_extractor
    if feature_extractor is None:
        feature_extractor = FeatureExtractor(cache)
    return feature_extractor


@router.get("/recommend/{user_id}", response_model=RecommendationResponse)
async def get_recommendations(
    user_id: str,
    k: int = Query(default=20, ge=1, le=100, description="Number of recommendations"),
    context: str = Query(default="home", description="Context: home, detail, search, similar"),
    retrain: bool = Query(default=False, description="Trigger immediate retrain before generating recommendations"),
    session: AsyncSession = Depends(get_db),
    engine: RecommendationEngine = Depends(get_recommendation_engine),
    extractor: FeatureExtractor = Depends(get_feature_extractor)
):
    """
    Get personalized movie recommendations for a user
    
    - **user_id**: User identifier
    - **k**: Number of recommendations to return (default: 20, max: 100)
    - **context**: Recommendation context (home, detail, search, similar)
    
    Returns a list of recommended movies with scores and reasons.
    """
    try:
        # Get user features (cache disabled)
        # features = await cache.get_user_features(user_id)
        # cached = features is not None
        
        # if features is None:
            # Try database
        from sqlalchemy import select
        from app.models import UserFeatures
        
        result = await session.execute(
            select(UserFeatures).where(UserFeatures.user_id == user_id)
        )
        row = result.scalar_one_or_none()
        
        features = None
        if row:
            features = row.features
            # Cache for next time
            # await cache.set_user_features(user_id, features)
        # New user - use default features
        if features is None:
            features = {
                "totalInteractions": 0,
                "watchHistory": [],
                "favorites": [],
                "ratings": {}
            }
        
        # Get recommendations
        # Optionally trigger retrain in background (non-blocking) to avoid client timeouts
        if retrain:
            try:
                logger.info("Retrain requested for user %s - scheduling background retrain", user_id)
                # schedule the engine's offline retrain in background; it will train and reload when done
                try:
                    asyncio.create_task(engine._schedule_retrain())
                except Exception:
                    # Fallback: call trainer in thread but do not await reload to avoid blocking
                    asyncio.create_task(asyncio.to_thread(
                        model_trainer.train_and_save_from_db,
                        settings.database_url,
                        engine.model_path,
                    ))

                # Invalidate any cached recommendations for this user so we compute fresh list next time
                try:
                    await global_cache.invalidate_recommendations(user_id)
                except Exception:
                    logger.debug("Failed to invalidate cache for user %s", user_id)
            except Exception:
                logger.exception("Failed to schedule retrain for user %s", user_id)

        try:
                recommendations = await engine.get_recommendations(
                    user_id=user_id,
                    features=features,
                    k=k,
                    context=context
                )
        except Exception as e:
            logger.exception("Recommendation engine failed for user %s: %s", user_id, e)
            # Fail open for downstream callers: return empty recommendations
            return RecommendationResponse(
                userId=user_id,
                modelVersion=getattr(engine, "model_version", ""),
                cached=False,
                recommendations=[],
                generatedAt=datetime.utcnow(),
                context=context,
            )
        
        # Format response
        rec_items = [
            {"movieId": r["movieId"], "score": r["score"], "reasons": r.get("reasons", [])}
            for r in recommendations
        ]

        payload = {
            "userId": user_id,
            "modelVersion": engine.model_version,
            "cached": False,  # Cache disabled
            "recommendations": rec_items,
            "generatedAt": datetime.now(timezone.utc).isoformat(),
            "context": context,
        }

        return JSONResponse(content=payload)
        
    except Exception as e:
        logger.exception("Failed to get recommendations for user %s: %s", user_id, e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get recommendations: {str(e)}"
        )


@router.get("/features/{user_id}", response_model=UserFeaturesResponse)
async def get_user_features(
    user_id: str,
    session: AsyncSession = Depends(get_db)
):
    """
    Get user features from cache or database
    
    - **user_id**: User identifier
    
    Returns the user's feature vector used for recommendations.
    """
    try:
        # Try cache first
        features = await cache.get_user_features(user_id)
        
        if features is None:
            # Fallback to database
            from sqlalchemy import select
            from app.models import UserFeatures
            
            from sqlalchemy import cast, String

            result = await session.execute(
                select(UserFeatures).where(cast(UserFeatures.user_id, String) == user_id)
            )
            row = result.scalar_one_or_none()
            
            if row:
                features = row.features
                updated_at = row.updated_at
                version = row.version
            else:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail=f"Features not found for user {user_id}"
                )
        else:
            updated_at = datetime.utcnow()
            version = settings.model_version
        
        return UserFeaturesResponse(
            userId=user_id,
            features=features,
            version=version,
            updatedAt=updated_at
        )
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get user features: {str(e)}"
        )