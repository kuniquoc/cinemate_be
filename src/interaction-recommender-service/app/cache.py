"""
Redis cache client for features and recommendations
"""
import json
import logging
from datetime import timedelta
from typing import Optional, Dict, Any

import redis.asyncio as redis

from app.config import get_settings

logger = logging.getLogger(__name__)

settings = get_settings()


class RedisCache:
    """Redis cache manager for features and recommendations"""
    
    def __init__(self):
        self.client: Optional[redis.Redis] = None
        self._connected = False
    
    async def connect(self) -> None:
        """Initialize Redis connection"""
        try:
            self.client = redis.from_url(
                settings.redis_url,
                encoding="utf-8",
                decode_responses=True,
            )
            await self.client.ping()
            self._connected = True
            logger.info("Redis connection established")
        except Exception as e:
            logger.error(f"Failed to connect to Redis: {e}")
            self._connected = False
            raise
    
    async def disconnect(self) -> None:
        """Close Redis connection"""
        if self.client:
            await self.client.close()
            self._connected = False
            logger.info("Redis connection closed")
    
    async def health_check(self) -> Dict[str, Any]:
        """Check Redis health"""
        try:
            if not self.client or not self._connected:
                return {"status": "disconnected", "message": "Not connected"}
            
            latency = await self.client.ping()
            return {"status": "healthy", "latency_ms": latency}
        except Exception as e:
            return {"status": "unhealthy", "message": str(e)}
    
    # ============== User Features ==============
    
    async def get_user_features(self, user_id: str) -> Optional[Dict[str, Any]]:
        """Get user features from cache"""
        try:
            key = f"user:features:{user_id}"
            data = await self.client.get(key)
            if data:
                return json.loads(data)
            return None
        except Exception as e:
            logger.error(f"Error getting user features for {user_id}: {e}")
            return None
    
    async def set_user_features(
        self, 
        user_id: str, 
        features: Dict[str, Any],
        ttl: Optional[int] = None
    ) -> bool:
        """Set user features in cache"""
        try:
            key = f"user:features:{user_id}"
            ttl = ttl or settings.redis_feature_ttl
            await self.client.setex(
                key,
                timedelta(seconds=ttl),
                json.dumps(features)
            )
            return True
        except Exception as e:
            logger.error(f"Error setting user features for {user_id}: {e}")
            return False
    
    async def delete_user_features(self, user_id: str) -> bool:
        """Delete user features from cache"""
        try:
            key = f"user:features:{user_id}"
            await self.client.delete(key)
            return True
        except Exception as e:
            logger.error(f"Error deleting user features for {user_id}: {e}")
            return False
    
    # ============== Recommendations Cache ==============
    
    async def get_recommendations(
        self, 
        user_id: str, 
        context: str = "default"
    ) -> Optional[Dict[str, Any]]:
        """Get cached recommendations"""
        try:
            key = f"recommendations:{user_id}:{context}"
            data = await self.client.get(key)
            if data:
                return json.loads(data)
            return None
        except Exception as e:
            logger.error(f"Error getting recommendations for {user_id}: {e}")
            return None
    
    async def set_recommendations(
        self,
        user_id: str,
        recommendations: Dict[str, Any],
        context: str = "default",
        ttl: Optional[int] = None
    ) -> bool:
        """Cache recommendations"""
        try:
            key = f"recommendations:{user_id}:{context}"
            ttl = ttl or settings.redis_cache_ttl
            await self.client.setex(
                key,
                timedelta(seconds=ttl),
                json.dumps(recommendations)
            )
            return True
        except Exception as e:
            logger.error(f"Error caching recommendations for {user_id}: {e}")
            return False
    
    async def invalidate_recommendations(self, user_id: str) -> bool:
        """Invalidate all cached recommendations for a user"""
        try:
            pattern = f"recommendations:{user_id}:*"
            cursor = 0
            while True:
                cursor, keys = await self.client.scan(cursor, match=pattern)
                if keys:
                    await self.client.delete(*keys)
                if cursor == 0:
                    break
            return True
        except Exception as e:
            logger.error(f"Error invalidating recommendations for {user_id}: {e}")
            return False
    
    # ============== Event Deduplication ==============
    
    async def check_request_processed(self, request_id: str) -> bool:
        """Check if request was already processed (idempotency)"""
        try:
            key = f"processed_request:{request_id}"
            result = await self.client.exists(key)
            return result > 0
        except Exception as e:
            logger.error(f"Error checking request {request_id}: {e}")
            return False
    
    async def mark_request_processed(
        self, 
        request_id: str, 
        ttl: int = 86400  # 24 hours
    ) -> bool:
        """Mark request as processed"""
        try:
            key = f"processed_request:{request_id}"
            await self.client.setex(key, timedelta(seconds=ttl), "1")
            return True
        except Exception as e:
            logger.error(f"Error marking request {request_id}: {e}")
            return False
    
    # ============== Rate Limiting ==============
    
    async def check_rate_limit(
        self, 
        identifier: str, 
        limit: int = 100, 
        window: int = 60
    ) -> bool:
        """Check rate limit for an identifier"""
        try:
            key = f"rate_limit:{identifier}"
            current = await self.client.incr(key)
            if current == 1:
                await self.client.expire(key, window)
            return current <= limit
        except Exception as e:
            logger.error(f"Error checking rate limit for {identifier}: {e}")
            return True  # Allow on error


# Global cache instance
cache = RedisCache()


async def get_cache() -> RedisCache:
    """Get cache instance"""
    return cache
