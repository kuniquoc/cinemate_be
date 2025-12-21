import asyncio
import os
from typing import Any, Optional

from app.config import get_settings

settings = get_settings()


class SimpleCache:
    def __init__(self):
        self._store = {}
        self._lock = asyncio.Lock()
        self._redis = None

    async def connect(self):
        # Try to use redis if REDIS_URL is provided
        redis_url = settings.redis_url
        if redis_url:
            try:
                import redis.asyncio as redis
                self._redis = redis.from_url(redis_url, decode_responses=True)
                # test ping
                await self._redis.ping()
                return True
            except Exception:
                # fallback to in-memory store
                self._redis = None
                return True

        return True

    async def disconnect(self):
        if self._redis:
            try:
                await self._redis.close()
            except Exception:
                pass
        return True

    # User features
    async def get_user_features(self, user_id: str) -> Optional[Any]:
        if self._redis:
            try:
                import json
                raw = await self._redis.get(f"features:{user_id}")
                return json.loads(raw) if raw else None
            except Exception:
                return None

        async with self._lock:
            return self._store.get(f"features:{user_id}")

    async def set_user_features(self, user_id: str, features: Any):
        if self._redis:
            try:
                import json
                await self._redis.set(f"features:{user_id}", json.dumps(features))
                return
            except Exception:
                pass

        async with self._lock:
            self._store[f"features:{user_id}"] = features

    # Recommendations cache
    async def invalidate_recommendations(self, user_id: str):
        if self._redis:
            try:
                await self._redis.delete(f"recs:{user_id}")
                return
            except Exception:
                pass

        async with self._lock:
            keys = [k for k in self._store.keys() if k.startswith(f"recs:{user_id}")]
            for k in keys:
                self._store.pop(k, None)

    async def get_recommendations(self, user_id: str) -> Optional[Any]:
        if self._redis:
            try:
                import json
                raw = await self._redis.get(f"recs:{user_id}")
                return json.loads(raw) if raw else None
            except Exception:
                return None

        async with self._lock:
            return self._store.get(f"recs:{user_id}")

    async def set_recommendations(self, user_id: str, recs: Any):
        if self._redis:
            try:
                import json
                await self._redis.set(f"recs:{user_id}", json.dumps(recs))
                return
            except Exception:
                pass

        async with self._lock:
            self._store[f"recs:{user_id}"] = recs

    # Movies cache (list of movie dicts)
    async def get_movies(self) -> Optional[Any]:
        if self._redis:
            try:
                import json
                raw = await self._redis.get("movies:all")
                return json.loads(raw) if raw else None
            except Exception:
                return None

        async with self._lock:
            return self._store.get("movies:all")

    async def set_movies(self, movies: Any):
        if self._redis:
            try:
                import json
                # cache for 1 hour
                await self._redis.set("movies:all", json.dumps(movies), ex=3600)
                return
            except Exception:
                pass

        async with self._lock:
            self._store["movies:all"] = movies


cache = SimpleCache()
