import os
from types import SimpleNamespace


def get_settings():
    # Read from environment where possible; provide sensible defaults for local dev
    database_url = os.environ.get(
        "DATABASE_URL",
        os.environ.get("INTERACTION_DATABASE_URL", "sqlite+aiosqlite:///./recommender.db")
    )
    redis_url = os.environ.get("REDIS_URL", os.environ.get("REDIS", None))
    model_path = os.environ.get("MODEL_PATH", "./models/recommender_model.joblib")

    return SimpleNamespace(
        app_version=os.environ.get("APP_VERSION", "0.1.0"),
        app_name=os.environ.get("APP_NAME", "interaction-recommender-service"),
        host=os.environ.get("HOST", "0.0.0.0"),
        port=int(os.environ.get("PORT", 8000)),
        workers=int(os.environ.get("WORKERS", 1)),
        debug=os.environ.get("DEBUG", "true").lower() in ("1", "true", "yes"),
        enable_kafka=False,
        enable_consumers=False,
        enable_feature_extraction=False,
        database_url=database_url,
        redis_url=redis_url,
        movie_service_url=os.environ.get("MOVIE_SERVICE_URL", "http://movie-service:8080"),
        model_path=model_path,
        model_version=os.environ.get("MODEL_VERSION", "v0"),
    )
