"""
Application configuration using Pydantic Settings
"""
from functools import lru_cache
from typing import Optional
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables"""
    
    # Application
    app_name: str = "interaction-recommender-service"
    app_version: str = "1.0.0"
    debug: bool = False
    
    # Database
    database_url: str = "postgresql+asyncpg://admin:admin@interaction-postgres:5432/interaction_db"
    db_pool_size: int = 10
    db_max_overflow: int = 20
    
    # Redis
    redis_url: str = "redis://cinemate-redis:6379"
    redis_cache_ttl: int = 3600  # 1 hour
    redis_feature_ttl: int = 86400  # 24 hours
    
    # Kafka
    kafka_bootstrap_servers: str = "cinemate-broker:29092"
    kafka_consumer_group: str = "ir-consumers"
    kafka_interaction_topic: str = "interaction_events"
    kafka_features_topic: str = "processed_features"
    kafka_feedback_topic: str = "model_feedback"
    kafka_dlq_topic: str = "interaction_events_dlq"
    
    # Feature flags
    enable_consumers: bool = True
    enable_kafka: bool = True
    enable_feature_extraction: bool = True
    
    # Model
    model_path: str = "/app/models/recommender.pkl"
    model_version: str = "v1.0.0"
    default_recommendation_count: int = 20
    
    # Retry settings
    max_retries: int = 3
    retry_delay_seconds: float = 1.0
    
    # Server
    host: str = "0.0.0.0"
    port: int = 8000
    workers: int = 2
    
    # Database connection retry
    db_retry_count: int = 10
    db_retry_delay: float = 3.0

    class Config:
        env_file = ".env"
        case_sensitive = False
        protected_namespaces = ('settings_',)  # Fix Pydantic warning for model_* fields


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance"""
    return Settings()
