"""
API Routes for health checks and system management
"""
import time
from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, status

from app.database import engine
from app.cache import cache
from app.kafka_client import kafka_manager
from app.routes.recommendations import get_recommendation_engine
from app.recommendation_engine import RecommendationEngine
from app.schemas import (
    HealthResponse,
    ComponentHealth,
    ModelInfo,
    ReloadModelResponse
)
from app.config import get_settings

router = APIRouter(prefix="/api/v1", tags=["System"])

settings = get_settings()


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    Check health of all service components
    
    Returns status of:
    - Database connection
    - Redis cache
    - Kafka producer/consumer
    - ML model
    """
    components = {}
    overall_status = "healthy"
    
    # Check database
    try:
        start = time.time()
        async with engine.connect() as conn:
            await conn.execute("SELECT 1")
        latency = (time.time() - start) * 1000
        components["database"] = ComponentHealth(
            status="healthy",
            latencyMs=round(latency, 2)
        )
    except Exception as e:
        components["database"] = ComponentHealth(
            status="unhealthy",
            message=str(e)
        )
        overall_status = "degraded"
    
    # Check Redis
    try:
        start = time.time()
        await cache.client.ping()
        latency = (time.time() - start) * 1000
        components["redis"] = ComponentHealth(
            status="healthy",
            latencyMs=round(latency, 2)
        )
    except Exception as e:
        components["redis"] = ComponentHealth(
            status="unhealthy",
            message=str(e)
        )
        overall_status = "degraded"
    
    # Check Kafka
    kafka_health = await kafka_manager.health_check()
    if kafka_health.get("status") == "disabled":
        components["kafka"] = ComponentHealth(
            status="disabled",
            message="Kafka is disabled"
        )
    elif kafka_health.get("status") == "healthy":
        components["kafka"] = ComponentHealth(status="healthy")
    else:
        components["kafka"] = ComponentHealth(
            status="unhealthy",
            message=str(kafka_health.get("details"))
        )
        overall_status = "degraded"
    
    # Check model
    try:
        engine = get_recommendation_engine()
        model_info = engine.get_model_info()
        components["model"] = ComponentHealth(
            status="healthy" if model_info["status"] == "loaded" else "fallback",
            message=f"Version: {model_info['version']}"
        )
    except Exception as e:
        components["model"] = ComponentHealth(
            status="unhealthy",
            message=str(e)
        )
        overall_status = "degraded"
    
    return HealthResponse(
        status=overall_status,
        version=settings.app_version,
        timestamp=datetime.utcnow(),
        components=components
    )


@router.get("/health/live")
async def liveness_check():
    """
    Kubernetes liveness probe
    Returns 200 if service is running
    """
    return {"status": "alive", "timestamp": datetime.utcnow().isoformat()}


@router.get("/health/ready")
async def readiness_check():
    """
    Kubernetes readiness probe
    Returns 200 if service is ready to accept traffic
    """
    # Check critical components
    try:
        # Database
        async with engine.connect() as conn:
            await conn.execute("SELECT 1")
        
        # Redis
        await cache.client.ping()
        
        return {"status": "ready", "timestamp": datetime.utcnow().isoformat()}
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"Service not ready: {str(e)}"
        )


@router.get("/model/info", response_model=ModelInfo)
async def get_model_info(
    engine: RecommendationEngine = Depends(get_recommendation_engine)
):
    """
    Get information about the loaded recommendation model
    """
    info = engine.get_model_info()
    return ModelInfo(
        version=info["version"],
        path=info["path"],
        loadedAt=datetime.fromisoformat(info["loaded_at"]) if info["loaded_at"] else datetime.utcnow(),
        status=info["status"]
    )


@router.post("/model/reload", response_model=ReloadModelResponse)
async def reload_model(
    model_path: Optional[str] = None,
    engine: RecommendationEngine = Depends(get_recommendation_engine)
):
    """
    Reload the recommendation model
    
    - **model_path**: Optional new model path (uses default if not provided)
    
    This endpoint allows hot-reloading of the ML model without restarting the service.
    """
    try:
        result = await engine.reload_model(model_path)
        
        if not result["success"]:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to load model, using fallback"
            )
        
        return ReloadModelResponse(
            status="reloaded",
            previousVersion=result["previous_version"],
            newVersion=result["new_version"],
            reloadedAt=result["reloaded_at"]
        )
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to reload model: {str(e)}"
        )


@router.get("/config")
async def get_config():
    """
    Get service configuration (non-sensitive)
    """
    return {
        "appName": settings.app_name,
        "appVersion": settings.app_version,
        "modelVersion": settings.model_version,
        "enableConsumers": settings.enable_consumers,
        "enableKafka": settings.enable_kafka,
        "defaultRecommendationCount": settings.default_recommendation_count,
        "kafkaTopics": {
            "interactions": settings.kafka_interaction_topic,
            "features": settings.kafka_features_topic,
            "feedback": settings.kafka_feedback_topic
        }
    }
