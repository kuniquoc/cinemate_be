"""
API Routes for recommendations
"""
from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.cache import cache
from app.recommendation_engine import RecommendationEngine
from app.feature_extractor import FeatureExtractor
from app.schemas import (
    RecommendationResponse,
    RecommendationItem,
    UserFeaturesResponse
)
from app.config import get_settings

router = APIRouter(prefix="/api/v1", tags=["Recommendations"])

settings = get_settings()

# Initialize engines (will be properly initialized in main.py)
recommendation_engine: Optional[RecommendationEngine] = None
feature_extractor: Optional[FeatureExtractor] = None


def get_recommendation_engine() -> RecommendationEngine:
    """Get recommendation engine instance"""
    global recommendation_engine
    if recommendation_engine is None:
        recommendation_engine = RecommendationEngine(cache)
    return recommendation_engine


def get_feature_extractor() -> FeatureExtractor:
    """Get feature extractor instance"""
    global feature_extractor
    if feature_extractor is None:
        feature_extractor = FeatureExtractor(cache)
    return feature_extractor


@router.get("/recommend/{user_id}", response_model=RecommendationResponse)
async def get_recommendations(
    user_id: str,
    k: int = Query(default=20, ge=1, le=100, description="Number of recommendations"),
    context: str = Query(default="home", description="Context: home, detail, search, similar"),
    session: AsyncSession = Depends(get_db),
    engine: RecommendationEngine = Depends(get_recommendation_engine),
    extractor: FeatureExtractor = Depends(get_feature_extractor)
):
    """
    Get personalized movie recommendations for a user
    
    - **user_id**: User identifier
    - **k**: Number of recommendations to return (default: 20, max: 100)
    - **context**: Recommendation context (home, detail, search, similar)
    
    Returns a list of recommended movies with scores and reasons.
    """
    try:
        # Get user features
        features = await cache.get_user_features(user_id)
        cached = features is not None
        
        if features is None:
            # Try database
            from sqlalchemy import select
            from app.models import UserFeatures
            
            result = await session.execute(
                select(UserFeatures).where(UserFeatures.user_id == user_id)
            )
            row = result.scalar_one_or_none()
            
            if row:
                features = row.features
                # Cache for next time
                await cache.set_user_features(user_id, features)
            else:
                # New user - use default features
                features = {
                    "totalInteractions": 0,
                    "watchHistory": [],
                    "favorites": [],
                    "ratings": {}
                }
        
        # Get recommendations
        recommendations = await engine.get_recommendations(
            user_id=user_id,
            features=features,
            k=k,
            context=context
        )
        
        # Format response
        rec_items = [
            RecommendationItem(
                movieId=r["movieId"],
                score=r["score"],
                reasons=r.get("reasons", [])
            )
            for r in recommendations
        ]
        
        return RecommendationResponse(
            userId=user_id,
            modelVersion=engine.model_version,
            cached=cached,
            recommendations=rec_items,
            generatedAt=datetime.utcnow(),
            context=context
        )
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get recommendations: {str(e)}"
        )


@router.get("/features/{user_id}", response_model=UserFeaturesResponse)
async def get_user_features(
    user_id: str,
    session: AsyncSession = Depends(get_db)
):
    """
    Get user features from cache or database
    
    - **user_id**: User identifier
    
    Returns the user's feature vector used for recommendations.
    """
    try:
        # Try cache first
        features = await cache.get_user_features(user_id)
        
        if features is None:
            # Fallback to database
            from sqlalchemy import select
            from app.models import UserFeatures
            
            result = await session.execute(
                select(UserFeatures).where(UserFeatures.user_id == user_id)
            )
            row = result.scalar_one_or_none()
            
            if row:
                features = row.features
                updated_at = row.updated_at
                version = row.version
            else:
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail=f"Features not found for user {user_id}"
                )
        else:
            updated_at = datetime.utcnow()
            version = settings.model_version
        
        return UserFeaturesResponse(
            userId=user_id,
            features=features,
            version=version,
            updatedAt=updated_at
        )
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get user features: {str(e)}"
        )


@router.post("/features/{user_id}/refresh")
async def refresh_user_features(
    user_id: str,
    days_back: int = Query(default=90, ge=1, le=365),
    session: AsyncSession = Depends(get_db),
    extractor: FeatureExtractor = Depends(get_feature_extractor)
):
    """
    Recompute user features from historical events
    
    - **user_id**: User identifier
    - **days_back**: Number of days of history to consider (default: 90)
    
    Triggers a full feature recomputation for the user.
    """
    try:
        features = await extractor.compute_full_features(
            user_id=user_id,
            session=session,
            days_back=days_back
        )
        
        return {
            "userId": user_id,
            "status": "refreshed",
            "featureCount": len(features),
            "refreshedAt": datetime.utcnow().isoformat() + "Z"
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to refresh features: {str(e)}"
        )
