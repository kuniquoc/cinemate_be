"""
Routes package initialization
"""
from app.routes.events import router as events_router
from app.routes.recommendations import router as recommendations_router
from app.routes.health import router as health_router

__all__ = [
    "events_router",
    "recommendations_router",
    "health_router"
]
