"""Simple health routes used by the service."""
from datetime import datetime
from fastapi import APIRouter

router = APIRouter(prefix="/api/v1", tags=["System"])


@router.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "version": "0.1.0",
        "timestamp": datetime.utcnow().isoformat(),
        "components": {"database": "ok", "redis": "ok"}
    }


@router.get("/health/live")
async def liveness_check():
    return {"status": "alive", "timestamp": datetime.utcnow().isoformat()}


@router.get("/health/ready")
async def readiness_check():
    return {"status": "ready", "timestamp": datetime.utcnow().isoformat()}
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
