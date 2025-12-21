import asyncio
import logging
from typing import Any, Dict, List
import os

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.decomposition import TruncatedSVD
import joblib

from app.cache import cache
from app.database import AsyncSessionLocal
from app.models import Movie, Rating
from app.config import get_settings

settings = get_settings()
logger = logging.getLogger(__name__)


class RecommendationEngine:
    def __init__(self, cache_client=None):
        self.cache = cache_client or cache
        self.model_version = settings.model_version
        self.movies = []
        self.movie_index = {}
        self.tfidf = None
        self.tfidf_matrix = None
        self.user_index = {}
        self.svd = None
        self._monitor_task = None
        self.model_path = settings.model_path

    async def load_model(self):
        # Disabled loading persisted model to always use fresh data
        # if os.path.exists(self.model_path):
        #     try:
        #         data = joblib.load(self.model_path)
        #         self.movies = data.get("movies", [])
        #         self.tfidf = data.get("tfidf")
        #         self.tfidf_matrix = data.get("tfidf_matrix")
        #         self.svd = data.get("svd")
        #         self.user_index = data.get("user_ids_map", {})
        #         self.movie_index = {m["movie_id"]: i for i, m in enumerate(self.movies)}
        #         logger.info("Loaded persisted recommender model from %s", self.model_path)
        #         # ensure persisted model includes movies
        #         if not self.movies:
        #             raise RuntimeError("Persisted model contains no movies; DB must contain movie data")
        #         return
        #     except Exception as e:
        #         logger.debug("Failed to load model file: %s", e)
        # Otherwise fetch movie list from movie service and persist; require movie service to return data
        await self._build_from_service_and_save()

    async def _build_from_service_and_save(self):
        # Try cache first
        movies = await self.cache.get_movies()
        if movies:
            self.movies = movies
        else:
            # call movie service
            import httpx
            settings = get_settings()
            base = settings.movie_service_url.rstrip("/")
            url = base + "/api/v1/movies"
            try:
                async with httpx.AsyncClient(timeout=10.0) as client:
                    resp = await client.get(url)
                    if resp.status_code >= 400:
                        raise RuntimeError(f"movie service returned {resp.status_code}")
                    data = resp.json()
                    # expect list of movie dicts; be tolerant of different API shapes
                    def _extract_movies(obj):
                        # direct list
                        if isinstance(obj, list):
                            return obj
                        if not isinstance(obj, dict):
                            return None
                        # common container keys
                        for key in ("movies", "data", "results", "items"):
                            v = obj.get(key)
                            if isinstance(v, list):
                                return v
                        # try to find any list-of-dicts value that looks like movies
                        for v in obj.values():
                            if isinstance(v, list) and v:
                                first = v[0]
                                if isinstance(first, dict) and any(k in first for k in ("movie_id", "id", "title")):
                                    return v
                        return None

                    movies_list = _extract_movies(data)
                    if movies_list is not None:
                        self.movies = movies_list
                        # Normalize movie dicts so we always have a `movie_id`, `title`,
                        # `genres`, and `overview` keys used throughout the engine.
                        normalized = []
                        for m in self.movies:
                            if not isinstance(m, dict):
                                continue
                            mid = m.get("movie_id") or m.get("movieId") or m.get("id") or m.get("movieID")
                            if mid is None:
                                logger.debug("Skipping movie entry without id: %s", m)
                                continue
                            norm = {
                                "movie_id": str(mid),
                                "title": m.get("title") or m.get("name") or "",
                                "genres": m.get("genres") or m.get("genre") or self._join_categories(m.get("categories", [])),
                                "overview": m.get("overview") or m.get("description") or "",
                            }
                            # preserve original fields for compatibility
                            norm.update(m)
                            normalized.append(norm)

                        self.movies = normalized
                    else:
                        logger.warning("Unexpected movie service response format; proceeding with empty movie list")
                        self.movies = []
            except Exception as e:
                logger.error(f"failed to fetch movies from movie service: {e}; proceeding with empty movie list")
                self.movies = []

            # cache movies
            await self.cache.set_movies(self.movies)

        # Build TF-IDF
        corpus = [((m.get("genres") or "") + " " + (m.get("overview") or "")) for m in self.movies]
        try:
            self.tfidf = TfidfVectorizer(stop_words="english")
            self.tfidf_matrix = self.tfidf.fit_transform(corpus).toarray()
        except Exception:
            self.tfidf = None
            self.tfidf_matrix = None

        # Diagnostic logging to help debug zero-score issues
        try:
            nonempty_count = sum(1 for c in corpus if c.strip())
            logger.info(
                "Recommender build: movies=%d, corpus_nonempty=%d, tfidf_shape=%s",
                len(self.movies),
                nonempty_count,
                None if self.tfidf_matrix is None else self.tfidf_matrix.shape,
            )
            if nonempty_count == 0 and self.movies:
                logger.warning("All movie corpus is empty; content-based recommendations will be zero. Sample movie: %s", self.movies[0] if self.movies else None)
                logger.warning("Sample corpus: %s", corpus[:2] if corpus else "No corpus")
        except Exception:
            logger.debug("Failed to log recommender diagnostics", exc_info=True)

        # Build index only for movies that have a valid id; skip others
        movie_index = {}
        for idx, m in enumerate(self.movies):
            if not isinstance(m, dict):
                continue
            mid = m.get("movie_id") or m.get("movieId") or m.get("id")
            if mid is None:
                logger.debug("Skipping movie without id when building index: %s", m)
                continue
            movie_index[str(mid)] = idx
        self.movie_index = movie_index

        # Build collaborative model
        await self._build_collaborative()

        # Save model to disk if possible
        try:
            payload = {
                "movies": self.movies,
                "tfidf": self.tfidf,
                "tfidf_matrix": self.tfidf_matrix,
                "svd": self.svd,
                "user_ids_map": self.user_index,
            }
            os.makedirs(os.path.dirname(self.model_path), exist_ok=True)
            joblib.dump(payload, self.model_path)
            logger.info("Saved recommender model to %s", self.model_path)
        except Exception as e:
            logger.debug("Failed to persist model: %s", e)

    async def _build_collaborative(self):
        # Load ratings from user_features instead of ratings table
        async with AsyncSessionLocal() as session:
            try:
                result = await session.execute(select(UserFeatures.user_id, UserFeatures.features))
                rows = []
                for row in result:
                    user_id = row[0]
                    features = row[1]
                    ratings = features.get("ratings", {})
                    for movie_id, rating in ratings.items():
                        rows.append({"user_id": user_id, "movie_id": movie_id, "rating": rating})
            except Exception:
                rows = []

        if not rows:
            self.svd = None
            return

        try:
            logger.info("Loaded %d ratings rows for collaborative build", len(rows))
        except Exception:
            logger.debug("Failed to log ratings rows count", exc_info=True)

        user_ids = list({r["user_id"] for r in rows})
        movie_ids = list({r["movie_id"] for r in rows})
        self.user_index = {u: i for i, u in enumerate(user_ids)}
        movie_index = {m: i for i, m in enumerate(movie_ids)}

        mat = np.zeros((len(user_ids), len(movie_ids)))
        for r in rows:
            try:
                u = self.user_index[r["user_id"]]
                m = movie_index[r["movie_id"]]
                mat[u, m] = r["rating"]
            except Exception:
                continue

        # Decompose with TruncatedSVD
        try:
            n_comp = min(20, max(1, min(mat.shape) - 1))
            svd = TruncatedSVD(n_components=n_comp)
            latent = svd.fit_transform(mat)
            self.svd = (svd, user_ids, movie_ids, latent)
        except Exception as e:
            logger.debug("SVD build failed: %s", e)
            self.svd = None

    async def start_periodic_monitor(self):
        return None

    async def stop_periodic_monitor(self):
        return None

    async def _schedule_retrain(self):
        try:
            await self._build_collaborative()
            logger.info("Scheduled retrain completed successfully")
        except Exception:
            logger.exception("Scheduled retrain failed", exc_info=True)

    async def get_recommendations(self, user_id: str, features: Dict[str, Any], k: int = 20, context: str = "home") -> List[Dict[str, Any]]:
        # Always recompute (cache disabled)
        # cached = await self.cache.get_recommendations(user_id)
        # if cached:
        #     return cached[:k]

        # Recompute content-based and collaborative on the fly if model not persisted
        if self.tfidf is None or self.tfidf_matrix is None:
            # try to rebuild content part
            await self.load_model()

        # Auto retrain collaborative always to ensure fresh data
        logger.info("Auto retraining collaborative model for fresh data...")
        await self._build_collaborative()

        # Determine last watched
        last_watch = None
        if features and features.get("watchHistory"):
            last_watch = features["watchHistory"][-1].get("movieId")

        content_scores = {}
        if self.tfidf_matrix is not None:
            # Build user profile from ratings in features
            user_ratings = features.get("ratings", {}) if features else {}
            if user_ratings:
                user_vector = np.zeros(self.tfidf_matrix.shape[1])
                total_weight = 0.0
                for movie_id, rating in user_ratings.items():
                    if movie_id in self.movie_index:
                        idx = self.movie_index[movie_id]
                        user_vector += self.tfidf_matrix[idx] * rating
                        total_weight += rating
                if total_weight > 0:
                    user_vector /= total_weight  # Normalize by total rating weight
                    try:
                        sim = cosine_similarity(user_vector.reshape(1, -1), self.tfidf_matrix).flatten()
                        for i, s in enumerate(sim):
                            content_scores[self.movies[i]["movie_id"]] = float(s)
                    except Exception as e:
                        logger.warning("Failed to compute content similarity: %s", e)
                        content_scores = {}
            else:
                # Fallback to last_watch or favorites if no ratings
                last_watch = None
                if features and features.get("watchHistory"):
                    last_watch = features["watchHistory"][-1].get("movieId")
                if last_watch and last_watch in self.movie_index:
                    idx = self.movie_index[last_watch]
                    sim = cosine_similarity(self.tfidf_matrix[idx], self.tfidf_matrix).flatten()
                    for i, s in enumerate(sim):
                        content_scores[self.movies[i]["movie_id"]] = float(s)
                else:
                    for fav in features.get("favorites", []) if features else []:
                        if fav in self.movie_index:
                            idx = self.movie_index[fav]
                            sim = cosine_similarity(self.tfidf_matrix[idx], self.tfidf_matrix).flatten()
                            for i, s in enumerate(sim):
                                content_scores[self.movies[i]["movie_id"]] = max(content_scores.get(self.movies[i]["movie_id"], 0), float(s))

        # Collaborative scores
        collab_scores = {}
        if self.svd:
            try:
                svd, user_ids, movie_ids, latent = self.svd
                if user_id in user_ids:
                    uidx = user_ids.index(user_id)
                    user_vec = latent[uidx]
                    movie_latent = svd.components_.T
                    scores = movie_latent.dot(user_vec)
                    for i, mid in enumerate(movie_ids):
                        collab_scores[mid] = float(scores[i])
            except Exception:
                logger.debug("Collaborative scoring failed", exc_info=True)
                collab_scores = {}

        logger.info("User %s: content_scores keys=%d, collab_scores keys=%d", user_id, len(content_scores), len(collab_scores))

        # Hybrid

        # Hybrid
        final_scores = {}
        for m in self.movie_index.keys():
            c = content_scores.get(m, 0.0)
            cf = collab_scores.get(m, None)
            if cf is not None:
                score = 0.6 * cf + 0.4 * c
            else:
                score = c
            final_scores[m] = float(score)

        # Keep zero scores if no signal is available; do not apply artificial fallback

        recs = sorted(final_scores.items(), key=lambda x: x[1], reverse=True)
        out = [{"movieId": movie_id, "score": float(score), "reasons": ["hybrid"]} for movie_id, score in recs[:k]]

        # await self.cache.set_recommendations(user_id, out)  # Cache disabled

        return out

    async def predict_rating(self, user_id: str, movie_id: str) -> float:
        """Predict rating for a user-movie pair using collaborative filtering."""
        if not self.svd:
            return 0.0
        try:
            svd, user_ids, movie_ids, latent = self.svd
            if user_id in user_ids and movie_id in movie_ids:
                uidx = user_ids.index(user_id)
                midx = movie_ids.index(movie_id)
                user_vec = latent[uidx]
                movie_vec = svd.components_.T[midx]
                score = movie_vec.dot(user_vec)
                return float(score)
            else:
                return 0.0
        except Exception:
            logger.debug("Failed to predict rating", exc_info=True)
            return 0.0

    def _join_categories(self, categories):
        """Join category names into a string for genres fallback."""
        if not categories:
            return ""
        names = []
        for cat in categories:
            if isinstance(cat, dict) and "name" in cat:
                names.append(cat["name"])
            elif isinstance(cat, str):
                names.append(cat)
        return " ".join(names)
