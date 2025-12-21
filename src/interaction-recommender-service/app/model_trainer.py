"""
Simple model trainer utilities.

Provides a synchronous `train_and_save_from_db(database_url, model_path)`
function used by the recommendations route fallback path. It builds the
collaborative portion of the model from the application's database and
saves a serialized model payload to `model_path`.

This is intentionally minimal — for production you should implement
proper training pipelines, locking, and error handling.
"""
import asyncio
import logging
import os
from typing import Optional

import joblib

from app.recommendation_engine import RecommendationEngine
from app.config import get_settings

logger = logging.getLogger(__name__)


def train_and_save_from_db(database_url: Optional[str], model_path: Optional[str]) -> None:
    """Train collaborative model from DB and persist to disk.

    This function is synchronous because it's intended to run in a
    background thread via `asyncio.to_thread` from the FastAPI app.
    """
    try:
        engine = RecommendationEngine()
        # Run the async collaborative build in a fresh event loop
        try:
            asyncio.run(engine._build_collaborative())
        except Exception as e:
            logger.exception("Collaborative build failed: %s", e)

        # Persist minimal payload similar to RecommendationEngine
        payload = {
            "movies": engine.movies,
            "tfidf": engine.tfidf,
            "tfidf_matrix": engine.tfidf_matrix,
            "svd": engine.svd,
            "user_ids_map": engine.user_index,
        }

        if not model_path:
            settings = get_settings()
            model_path = settings.model_path

        os.makedirs(os.path.dirname(model_path), exist_ok=True)
        joblib.dump(payload, model_path)
        logger.info("Model trained and saved to %s", model_path)
    except Exception as e:
        logger.exception("train_and_save_from_db failed: %s", e)
def train_and_save_from_db(database_url: str, model_path: str):
    # Placeholder trainer: in real deployments train offline models
    return True
"""
Synchronous helpers to train and persist a recommendation model from the
service database. These functions are intentionally synchronous so they can
be invoked from the async application with `asyncio.to_thread(...)`.
"""
from typing import Dict, Any, List, Tuple
import os
import joblib
import numpy as np
"""Minimal safe model trainer used by recommendations route.

This module intentionally keeps imports inside functions to avoid
import-time failures in lightweight development environments.
"""
import os
from typing import Dict, Any


def train_and_save_from_db(database_url: str, model_path: str) -> Dict[str, Any]:
    # Minimal no-op trainer: create model path and return metadata
    os.makedirs(os.path.dirname(model_path or "./models"), exist_ok=True)
    return {"trained_feedback_count": 0, "model_path": model_path or "", "status": "noop"}

