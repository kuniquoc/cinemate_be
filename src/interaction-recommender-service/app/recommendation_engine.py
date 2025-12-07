"""
Recommendation engine for movie suggestions
"""
import logging
import os
from datetime import datetime
from typing import Dict, Any, List, Optional
from pathlib import Path
import asyncio

import joblib
import numpy as np

from app.config import get_settings
from app.cache import RedisCache

logger = logging.getLogger(__name__)

settings = get_settings()


class RecommendationEngine:
    """
    ML-based recommendation engine
    Supports loading .pkl models and generating recommendations
    """
    
    def __init__(self, cache: RedisCache):
        self.cache = cache
        self.model = None
        self.model_version = settings.model_version
        self.model_path = settings.model_path
        self.loaded_at: Optional[datetime] = None
        self._lock = asyncio.Lock()
    
    async def load_model(self) -> bool:
        """Load recommendation model from disk"""
        async with self._lock:
            try:
                if os.path.exists(self.model_path):
                    self.model = joblib.load(self.model_path)
                    self.loaded_at = datetime.utcnow()
                    logger.info(f"Model loaded from {self.model_path}")
                    return True
                else:
                    logger.warning(f"Model file not found at {self.model_path}, using fallback")
                    self.model = None
                    self.loaded_at = datetime.utcnow()
                    return False
            except Exception as e:
                logger.error(f"Failed to load model: {e}")
                self.model = None
                return False
    
    async def reload_model(self, new_path: Optional[str] = None) -> Dict[str, Any]:
        """Reload model, optionally from a new path"""
        previous_version = self.model_version
        
        if new_path:
            self.model_path = new_path
        
        success = await self.load_model()
        
        if success:
            # Increment version
            self.model_version = f"v{datetime.utcnow().strftime('%Y.%m.%d')}-{datetime.utcnow().strftime('%H%M%S')}"
        
        return {
            "success": success,
            "previous_version": previous_version,
            "new_version": self.model_version,
            "reloaded_at": datetime.utcnow()
        }
    
    async def get_recommendations(
        self,
        user_id: str,
        features: Dict[str, Any],
        k: int = 20,
        context: str = "default",
        exclude_watched: bool = True
    ) -> List[Dict[str, Any]]:
        """
        Generate movie recommendations for a user
        
        Args:
            user_id: User identifier
            features: User feature vector
            k: Number of recommendations to return
            context: Context for recommendations (home, detail, search, etc.)
            exclude_watched: Whether to exclude already watched movies
            
        Returns:
            List of recommendation items with scores and reasons
        """
        # Check cache first
        cached = await self.cache.get_recommendations(user_id, context)
        if cached:
            recommendations = cached.get("recommendations", [])[:k]
            return recommendations
        
        # Generate recommendations
        if self.model is not None:
            recommendations = await self._model_based_recommendations(
                user_id, features, k, context, exclude_watched
            )
        else:
            # Fallback to heuristic-based recommendations
            recommendations = await self._heuristic_recommendations(
                user_id, features, k, context, exclude_watched
            )
        
        # Cache recommendations
        cache_data = {
            "recommendations": recommendations,
            "model_version": self.model_version,
            "generated_at": datetime.utcnow().isoformat()
        }
        await self.cache.set_recommendations(user_id, cache_data, context)
        
        return recommendations
    
    async def _model_based_recommendations(
        self,
        user_id: str,
        features: Dict[str, Any],
        k: int,
        context: str,
        exclude_watched: bool
    ) -> List[Dict[str, Any]]:
        """Generate recommendations using ML model"""
        try:
            # Prepare feature vector
            feature_vector = self._features_to_vector(features)
            
            # Get candidate movies (in production, this would query movie catalog)
            candidates = await self._get_candidate_movies(features, exclude_watched)
            
            if not candidates:
                return await self._heuristic_recommendations(
                    user_id, features, k, context, exclude_watched
                )
            
            # Score candidates using model
            scores = []
            for movie_id in candidates:
                try:
                    # Combine user features with movie features
                    # In production: query movie features from movie-service or cache
                    score = self._score_candidate(feature_vector, movie_id)
                    scores.append((movie_id, score))
                except Exception as e:
                    logger.debug(f"Error scoring movie {movie_id}: {e}")
                    continue
            
            # Sort by score and take top k
            scores.sort(key=lambda x: x[1], reverse=True)
            top_k = scores[:k]
            
            # Format results
            recommendations = []
            for movie_id, score in top_k:
                reasons = self._generate_reasons(features, movie_id, score)
                recommendations.append({
                    "movieId": movie_id,
                    "score": float(score),
                    "reasons": reasons
                })
            
            return recommendations
            
        except Exception as e:
            logger.error(f"Error in model-based recommendations: {e}")
            return await self._heuristic_recommendations(
                user_id, features, k, context, exclude_watched
            )
    
    async def _heuristic_recommendations(
        self,
        user_id: str,
        features: Dict[str, Any],
        k: int,
        context: str,
        exclude_watched: bool
    ) -> List[Dict[str, Any]]:
        """
        Fallback heuristic-based recommendations
        Uses user features to generate simple recommendations
        """
        recommendations = []
        
        # Get user preferences
        favorites = features.get("favorites", [])
        high_rated = features.get("highRatedMovies", [])
        watch_history = features.get("watchHistory", [])
        
        # Combine preference indicators
        preference_movies = set(favorites + high_rated)
        watched_set = set(watch_history) if exclude_watched else set()
        
        # Generate candidate scores based on similarity to preferences
        # In production, this would query similar movies from movie-service
        candidate_pool = await self._generate_fallback_candidates(k * 2)
        
        for i, movie_id in enumerate(candidate_pool):
            if movie_id in watched_set:
                continue
            
            # Simple scoring based on position and random factor
            base_score = 1.0 - (i / len(candidate_pool)) * 0.5
            
            # Boost if similar to favorites (placeholder logic)
            if movie_id in preference_movies:
                base_score = min(1.0, base_score + 0.2)
            
            reasons = ["popular", "trending"]
            if movie_id in preference_movies:
                reasons.append("similar_to_favorites")
            
            recommendations.append({
                "movieId": movie_id,
                "score": round(base_score, 4),
                "reasons": reasons
            })
            
            if len(recommendations) >= k:
                break
        
        return recommendations
    
    def _features_to_vector(self, features: Dict[str, Any]) -> np.ndarray:
        """Convert feature dict to numpy vector for model input"""
        # Extract numerical features
        vector = [
            features.get("totalInteractions", 0),
            features.get("watchCount", 0),
            features.get("searchCount", 0),
            features.get("ratingCount", 0),
            features.get("favoriteCount", 0),
            features.get("avgRating", 0),
            features.get("avgWatchDuration", 0),
            features.get("totalWatchTime", 0),
            len(features.get("watchHistory", [])),
            len(features.get("favorites", [])),
        ]
        return np.array(vector, dtype=np.float32)
    
    def _score_candidate(self, user_vector: np.ndarray, movie_id: str) -> float:
        """Score a candidate movie for a user"""
        try:
            if hasattr(self.model, 'predict_proba'):
                # Classification model
                # In production: combine with movie features
                score = self.model.predict_proba([user_vector])[0][1]
            elif hasattr(self.model, 'predict'):
                # Regression model
                score = self.model.predict([user_vector])[0]
            else:
                # Generic scoring
                score = np.random.random() * 0.5 + 0.5
            
            return float(min(1.0, max(0.0, score)))
        except Exception:
            return np.random.random() * 0.3 + 0.5
    
    async def _get_candidate_movies(
        self, 
        features: Dict[str, Any],
        exclude_watched: bool
    ) -> List[str]:
        """
        Get candidate movies for scoring
        In production, this would query movie-service or a candidate index
        """
        # Placeholder: generate fake movie IDs
        # In production: call movie-service API or use pre-computed candidate sets
        candidates = [f"movie-{i:04d}" for i in range(200)]
        
        if exclude_watched:
            watched = set(features.get("watchHistory", []))
            candidates = [m for m in candidates if m not in watched]
        
        return candidates
    
    async def _generate_fallback_candidates(self, count: int) -> List[str]:
        """Generate fallback candidate movie IDs"""
        # In production: query popular/trending movies from movie-service
        return [f"movie-{i:04d}" for i in range(count)]
    
    def _generate_reasons(
        self, 
        features: Dict[str, Any], 
        movie_id: str, 
        score: float
    ) -> List[str]:
        """Generate human-readable reasons for recommendation"""
        reasons = []
        
        if score > 0.8:
            reasons.append(f"cf:{score:.2f}")
        
        if movie_id in features.get("favorites", []):
            reasons.append("in_favorites")
        
        if features.get("avgRating", 0) > 4.0:
            reasons.append("matches_taste")
        
        if not reasons:
            reasons = ["collaborative_filtering", "content_based"]
        
        return reasons
    
    def get_model_info(self) -> Dict[str, Any]:
        """Get information about loaded model"""
        return {
            "version": self.model_version,
            "path": self.model_path,
            "loaded_at": self.loaded_at.isoformat() if self.loaded_at else None,
            "status": "loaded" if self.model is not None else "fallback"
        }
