"""
Feature extraction service for user interactions
"""
import logging
from datetime import datetime, timedelta
from typing import Dict, Any, Optional, List
from collections import defaultdict

from sqlalchemy import select, func, and_
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import InteractionEvent, UserFeatures
from app.cache import RedisCache
from app.config import get_settings

logger = logging.getLogger(__name__)

settings = get_settings()


class FeatureExtractor:
    """
    Extracts and computes user features from interaction events
    Features are used for recommendation model inference
    """
    
    def __init__(self, cache: RedisCache):
        self.cache = cache
    
    async def extract_features(
        self, 
        event_data: Dict[str, Any],
        session: AsyncSession
    ) -> Dict[str, Any]:
        """
        Extract features from a single event and update user profile
        
        Args:
            event_data: Raw event data from Kafka
            session: Database session
            
        Returns:
            Updated user features
        """
        user_id = event_data.get("userId")
        event_type = event_data.get("eventType", "").upper()
        
        if not user_id:
            logger.warning("Event missing userId, skipping feature extraction")
            return {}
        
        # Get existing features
        features = await self._get_user_features(user_id, session)
        
        # Update features based on event type
        if event_type == "WATCH":
            features = await self._process_watch_event(features, event_data)
        elif event_type == "SEARCH":
            features = await self._process_search_event(features, event_data)
        elif event_type == "RATING":
            features = await self._process_rating_event(features, event_data)
        elif event_type == "FAVORITE":
            features = await self._process_favorite_event(features, event_data)
        
        # Update metadata
        features["lastActivityAt"] = event_data.get("serverTimestamp", datetime.utcnow().isoformat())
        features["totalInteractions"] = features.get("totalInteractions", 0) + 1
        features["version"] = settings.model_version
        
        # Save features
        await self._save_user_features(user_id, features, session)
        
        return features
    
    async def _get_user_features(
        self, 
        user_id: str, 
        session: AsyncSession
    ) -> Dict[str, Any]:
        """Get user features from cache or database"""
        # Try cache first
        cached = await self.cache.get_user_features(user_id)
        if cached:
            return cached
        
        # Fallback to database
        result = await session.execute(
            select(UserFeatures).where(UserFeatures.user_id == user_id)
        )
        row = result.scalar_one_or_none()
        
        if row:
            return row.features
        
        # Return default features for new user
        return self._get_default_features()
    
    async def _save_user_features(
        self, 
        user_id: str, 
        features: Dict[str, Any],
        session: AsyncSession
    ) -> None:
        """Save user features to cache and database"""
        # Save to cache
        await self.cache.set_user_features(user_id, features)
        
        # Invalidate recommendations cache (features changed)
        await self.cache.invalidate_recommendations(user_id)
        
        # Save to database (upsert)
        from sqlalchemy.dialects.postgresql import insert
        
        stmt = insert(UserFeatures).values(
            user_id=user_id,
            features=features,
            version=settings.model_version,
            updated_at=datetime.utcnow()
        ).on_conflict_do_update(
            index_elements=['user_id'],
            set_={
                'features': features,
                'version': settings.model_version,
                'updated_at': datetime.utcnow()
            }
        )
        await session.execute(stmt)
        await session.commit()
    
    def _get_default_features(self) -> Dict[str, Any]:
        """Return default features for new users"""
        return {
            "totalInteractions": 0,
            "watchHistory": [],
            "watchedGenres": {},
            "totalWatchTime": 0,
            "avgWatchDuration": 0,
            "searchHistory": [],
            "ratings": {},
            "avgRating": 0,
            "favorites": [],
            "preferredDevices": {},
            "preferredQuality": {},
            "createdAt": datetime.utcnow().isoformat(),
            "lastActivityAt": None,
        }
    
    async def _process_watch_event(
        self, 
        features: Dict[str, Any], 
        event_data: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Process watch event and update features"""
        movie_id = event_data.get("movieId")
        metadata = event_data.get("metadata", {})
        
        if movie_id:
            # Update watch history (keep last 100)
            watch_history = features.get("watchHistory", [])
            if movie_id not in watch_history:
                watch_history.append(movie_id)
                if len(watch_history) > 100:
                    watch_history = watch_history[-100:]
            features["watchHistory"] = watch_history
            
            # Update watch count
            features["watchCount"] = features.get("watchCount", 0) + 1
        
        # Update watch duration stats
        watch_duration = metadata.get("watchDuration", 0)
        if watch_duration > 0:
            total_watch_time = features.get("totalWatchTime", 0) + watch_duration
            watch_count = features.get("watchCount", 1)
            features["totalWatchTime"] = total_watch_time
            features["avgWatchDuration"] = total_watch_time / watch_count
        
        # Update device preferences
        device = metadata.get("device")
        if device:
            devices = features.get("preferredDevices", {})
            devices[device] = devices.get(device, 0) + 1
            features["preferredDevices"] = devices
        
        # Update quality preferences
        quality = metadata.get("quality")
        if quality:
            qualities = features.get("preferredQuality", {})
            qualities[quality] = qualities.get(quality, 0) + 1
            features["preferredQuality"] = qualities
        
        return features
    
    async def _process_search_event(
        self, 
        features: Dict[str, Any], 
        event_data: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Process search event and update features"""
        metadata = event_data.get("metadata", {})
        query = metadata.get("query", "")
        
        if query:
            # Update search history (keep last 50)
            search_history = features.get("searchHistory", [])
            search_entry = {
                "query": query,
                "timestamp": event_data.get("serverTimestamp"),
                "resultsCount": metadata.get("resultsCount", 0)
            }
            search_history.append(search_entry)
            if len(search_history) > 50:
                search_history = search_history[-50:]
            features["searchHistory"] = search_history
            
            # Update search count
            features["searchCount"] = features.get("searchCount", 0) + 1
            
            # Extract keywords for interest analysis
            keywords = features.get("searchKeywords", {})
            for word in query.lower().split():
                if len(word) > 2:  # Skip short words
                    keywords[word] = keywords.get(word, 0) + 1
            features["searchKeywords"] = dict(
                sorted(keywords.items(), key=lambda x: x[1], reverse=True)[:50]
            )
        
        return features
    
    async def _process_rating_event(
        self, 
        features: Dict[str, Any], 
        event_data: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Process rating event and update features"""
        movie_id = event_data.get("movieId")
        metadata = event_data.get("metadata", {})
        rating = metadata.get("rating")
        
        if movie_id and rating is not None:
            # Update ratings
            ratings = features.get("ratings", {})
            ratings[movie_id] = rating
            features["ratings"] = ratings
            
            # Update rating stats
            all_ratings = list(ratings.values())
            features["ratingCount"] = len(all_ratings)
            features["avgRating"] = sum(all_ratings) / len(all_ratings)
            
            # Categorize user preferences based on ratings
            high_rated = [mid for mid, r in ratings.items() if r >= 4.0]
            features["highRatedMovies"] = high_rated[-50:]  # Keep last 50
        
        return features
    
    async def _process_favorite_event(
        self, 
        features: Dict[str, Any], 
        event_data: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Process favorite event and update features"""
        movie_id = event_data.get("movieId")
        metadata = event_data.get("metadata", {})
        action = metadata.get("action", "add").lower()
        
        if movie_id:
            favorites = features.get("favorites", [])
            
            if action == "add" and movie_id not in favorites:
                favorites.append(movie_id)
            elif action == "remove" and movie_id in favorites:
                favorites.remove(movie_id)
            
            features["favorites"] = favorites
            features["favoriteCount"] = len(favorites)
        
        return features
    
    async def compute_full_features(
        self, 
        user_id: str, 
        session: AsyncSession,
        days_back: int = 90
    ) -> Dict[str, Any]:
        """
        Compute full user features from historical events
        Used for batch processing or feature refresh
        """
        cutoff_date = datetime.utcnow() - timedelta(days=days_back)
        
        # Get all user events within time window
        result = await session.execute(
            select(InteractionEvent)
            .where(
                and_(
                    InteractionEvent.user_id == user_id,
                    InteractionEvent.server_timestamp >= cutoff_date
                )
            )
            .order_by(InteractionEvent.server_timestamp)
        )
        events = result.scalars().all()
        
        # Start with default features
        features = self._get_default_features()
        
        # Process all events
        for event in events:
            event_data = {
                "userId": str(event.user_id),
                "movieId": str(event.movie_id) if event.movie_id else None,
                "eventType": event.event_type.upper(),
                "metadata": event.event_data or {},
                "serverTimestamp": event.server_timestamp.isoformat()
            }
            
            if event.event_type.upper() == "WATCH":
                features = await self._process_watch_event(features, event_data)
            elif event.event_type.upper() == "SEARCH":
                features = await self._process_search_event(features, event_data)
            elif event.event_type.upper() == "RATING":
                features = await self._process_rating_event(features, event_data)
            elif event.event_type.upper() == "FAVORITE":
                features = await self._process_favorite_event(features, event_data)
        
        # Update metadata
        features["lastActivityAt"] = datetime.utcnow().isoformat()
        features["totalInteractions"] = len(events)
        features["version"] = settings.model_version
        
        # Save computed features
        await self._save_user_features(user_id, features, session)
        
        return features
