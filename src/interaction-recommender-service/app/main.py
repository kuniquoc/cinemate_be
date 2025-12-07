"""
Main FastAPI application entry point
Interaction Recommender Service for Cinemate Platform
"""
import asyncio
import logging
import signal
import sys
from contextlib import asynccontextmanager
from datetime import datetime, timezone
from typing import Dict, Any

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import get_settings
from app.database import init_db, close_db, get_db_session
from app.cache import cache
from app.kafka_client import kafka_manager
from app.feature_extractor import FeatureExtractor
from app.recommendation_engine import RecommendationEngine
from app.routes import (
    events_router,
    recommendations_router,
    feedback_router,
    health_router
)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger(__name__)

settings = get_settings()

# Global instances
recommendation_engine: RecommendationEngine = None
feature_extractor: FeatureExtractor = None


async def handle_kafka_message(data: Dict[str, Any]) -> None:
    """
    Handle incoming Kafka messages for feature extraction
    This is the consumer callback for interaction_events topic
    """
    global feature_extractor
    
    try:
        logger.debug(f"Processing Kafka message: {data.get('requestId', 'unknown')}")
        
        async with get_db_session() as session:
            await feature_extractor.extract_features(data, session)
            
            # Optionally publish processed features
            if settings.enable_feature_extraction:
                user_id = data.get("userId")
                features = await cache.get_user_features(user_id)
                if features:
                    await kafka_manager.publish_processed_features({
                        "userId": user_id,
                        "features": features,
                        "eventId": data.get("eventId"),
                        "processedAt": datetime.now(timezone.utc).isoformat()
                    })
        
        logger.debug(f"Message processed: {data.get('requestId', 'unknown')}")
        
    except Exception as e:
        logger.error(f"Error handling Kafka message: {e}")
        raise


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Application lifespan manager
    Handles startup and shutdown events
    """
    global recommendation_engine, feature_extractor
    
    logger.info("Starting Interaction Recommender Service...")
    
    # Initialize database
    try:
        await init_db()
        logger.info("Database initialized")
    except Exception as e:
        logger.error(f"Failed to initialize database: {e}")
        raise
    
    # Initialize Redis cache
    try:
        await cache.connect()
        logger.info("Redis cache connected")
    except Exception as e:
        logger.warning(f"Redis connection failed, continuing without cache: {e}")
    
    # Initialize recommendation engine
    recommendation_engine = RecommendationEngine(cache)
    await recommendation_engine.load_model()
    
    # Initialize feature extractor
    feature_extractor = FeatureExtractor(cache)
    
    # Update route dependencies
    from app.routes import recommendations
    recommendations.recommendation_engine = recommendation_engine
    recommendations.feature_extractor = feature_extractor
    
    # Initialize Kafka
    if settings.enable_kafka:
        try:
            await kafka_manager.start_producer()
            logger.info("Kafka producer started")
            
            if settings.enable_consumers:
                await kafka_manager.start_consumer(handle_kafka_message)
                logger.info("Kafka consumer started")
        except Exception as e:
            logger.warning(f"Kafka initialization failed, continuing without Kafka: {e}")
    
    logger.info(f"Service started - Version: {settings.app_version}")
    
    yield
    
    # Shutdown
    logger.info("Shutting down Interaction Recommender Service...")
    
    # Stop Kafka
    if settings.enable_kafka:
        await kafka_manager.stop_consumer()
        await kafka_manager.stop_producer()
    
    # Close Redis
    await cache.disconnect()
    
    # Close database
    await close_db()
    
    logger.info("Service shutdown complete")


# Create FastAPI application
app = FastAPI(
    title="Interaction Recommender Service",
    description="""
    Movie Recommendation Service for Cinemate Platform
    
    ## Features
    
    * **Event Tracking**: Track user interactions (watch, search, rating, favorite)
    * **Feature Extraction**: Real-time feature extraction from user interactions
    * **Recommendations**: ML-based personalized movie recommendations
    * **Feedback Collection**: Collect user feedback for model improvement
    
    ## API Groups
    
    * `/events/*` - Track user interaction events
    * `/recommend/{user_id}` - Get personalized recommendations
    * `/features/{user_id}` - Get/refresh user features
    * `/feedback` - Submit recommendation feedback
    * `/health` - Health checks and system status
    """,
    version=settings.app_version,
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure properly in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Global exception handler
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "error": "Internal server error",
            "message": str(exc) if settings.debug else "An unexpected error occurred",
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
    )


# Request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start_time = datetime.now(timezone.utc)
    
    response = await call_next(request)
    
    duration = (datetime.now(timezone.utc) - start_time).total_seconds() * 1000
    logger.info(
        f"{request.method} {request.url.path} - "
        f"Status: {response.status_code} - "
        f"Duration: {duration:.2f}ms"
    )
    
    return response


# Include routers
app.include_router(health_router)
app.include_router(events_router)
app.include_router(recommendations_router)
app.include_router(feedback_router)


# Root endpoint
@app.get("/", tags=["Root"])
async def root():
    """Root endpoint with service information"""
    return {
        "service": settings.app_name,
        "version": settings.app_version,
        "status": "running",
        "docs": "/docs",
        "health": "/health"
    }


# Signal handlers for graceful shutdown
def handle_signal(signum, frame):
    logger.info(f"Received signal {signum}, initiating shutdown...")
    sys.exit(0)


signal.signal(signal.SIGTERM, handle_signal)
signal.signal(signal.SIGINT, handle_signal)


if __name__ == "__main__":
    import uvicorn
    
    uvicorn.run(
        "app.main:app",
        host=settings.host,
        port=settings.port,
        workers=settings.workers,
        reload=settings.debug
    )
