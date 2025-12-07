"""
Pydantic schemas for API requests and responses
"""
from datetime import datetime
from typing import Optional, List, Dict, Any
from uuid import UUID
from pydantic import BaseModel, Field, field_validator


# ============== Base Schemas ==============

class TimestampMixin(BaseModel):
    """Mixin for timestamp fields"""
    client_timestamp: Optional[datetime] = Field(None, alias="clientTimestamp")
    
    class Config:
        populate_by_name = True


# ============== Event Schemas ==============

class WatchMetadata(BaseModel):
    """Metadata for watch events"""
    watch_duration: Optional[int] = Field(None, alias="watchDuration", description="Duration in seconds")
    device: Optional[str] = Field(None, description="Device type: web, mobile, tv")
    quality: Optional[str] = Field(None, description="Video quality: 480p, 720p, 1080p, 4k")
    session_id: Optional[str] = Field(None, alias="sessionId")
    progress_percent: Optional[float] = Field(None, alias="progressPercent", ge=0, le=100)
    
    class Config:
        populate_by_name = True


class SearchMetadata(BaseModel):
    """Metadata for search events"""
    query: str = Field(..., description="Search query string")
    results_count: Optional[int] = Field(None, alias="resultsCount")
    filters: Optional[Dict[str, Any]] = Field(None, description="Applied filters")
    session_id: Optional[str] = Field(None, alias="sessionId")
    
    class Config:
        populate_by_name = True


class RatingMetadata(BaseModel):
    """Metadata for rating events"""
    rating: float = Field(..., ge=0.5, le=5.0, description="Rating value 0.5-5.0")
    previous_rating: Optional[float] = Field(None, alias="previousRating")
    
    class Config:
        populate_by_name = True


class FavoriteMetadata(BaseModel):
    """Metadata for favorite events"""
    action: str = Field(..., description="add or remove")
    list_id: Optional[str] = Field(None, alias="listId")
    
    @field_validator("action")
    @classmethod
    def validate_action(cls, v: str) -> str:
        if v.lower() not in ("add", "remove"):
            raise ValueError("action must be 'add' or 'remove'")
        return v.lower()
    
    class Config:
        populate_by_name = True


class WatchEventRequest(TimestampMixin):
    """Request schema for watch events"""
    request_id: Optional[str] = Field(None, alias="requestId")
    user_id: str = Field(..., alias="userId")
    movie_id: str = Field(..., alias="movieId")
    metadata: WatchMetadata
    
    class Config:
        populate_by_name = True


class SearchEventRequest(TimestampMixin):
    """Request schema for search events"""
    request_id: Optional[str] = Field(None, alias="requestId")
    user_id: str = Field(..., alias="userId")
    movie_id: Optional[str] = Field(None, alias="movieId")
    metadata: SearchMetadata
    
    class Config:
        populate_by_name = True


class RatingEventRequest(TimestampMixin):
    """Request schema for rating events"""
    request_id: Optional[str] = Field(None, alias="requestId")
    user_id: str = Field(..., alias="userId")
    movie_id: str = Field(..., alias="movieId")
    metadata: RatingMetadata
    
    class Config:
        populate_by_name = True


class FavoriteEventRequest(TimestampMixin):
    """Request schema for favorite events"""
    request_id: Optional[str] = Field(None, alias="requestId")
    user_id: str = Field(..., alias="userId")
    movie_id: str = Field(..., alias="movieId")
    metadata: FavoriteMetadata
    
    class Config:
        populate_by_name = True


class EventResponse(BaseModel):
    """Response schema for event endpoints"""
    request_id: str = Field(..., alias="requestId")
    status: str = Field(default="accepted")
    server_timestamp: datetime = Field(..., alias="serverTimestamp")
    
    class Config:
        populate_by_name = True


# ============== Feature Schemas ==============

class UserFeaturesResponse(BaseModel):
    """Response schema for user features"""
    user_id: str = Field(..., alias="userId")
    features: Dict[str, Any]
    version: Optional[str] = None
    updated_at: datetime = Field(..., alias="updatedAt")
    
    class Config:
        populate_by_name = True


# ============== Recommendation Schemas ==============

class RecommendationItem(BaseModel):
    """Single recommendation item"""
    movie_id: str = Field(..., alias="movieId")
    score: float = Field(..., ge=0, le=1)
    reasons: Optional[List[str]] = Field(default_factory=list)
    
    class Config:
        populate_by_name = True


class RecommendationResponse(BaseModel):
    """Response schema for recommendations"""
    user_id: str = Field(..., alias="userId")
    model_version: str = Field(..., alias="modelVersion")
    cached: bool = False
    recommendations: List[RecommendationItem]
    generated_at: datetime = Field(..., alias="generatedAt")
    context: Optional[str] = None
    
    class Config:
        populate_by_name = True


# ============== Feedback Schemas ==============

class FeedbackRequest(BaseModel):
    """Request schema for feedback"""
    user_id: str = Field(..., alias="userId")
    model_version: str = Field(..., alias="modelVersion")
    impression_list: List[str] = Field(..., alias="impressionList")
    clicked_item_id: Optional[str] = Field(None, alias="clickedItemId")
    watch_time_sec: Optional[int] = Field(None, alias="watchTimeSec")
    timestamp: datetime
    context: Optional[str] = None
    
    class Config:
        populate_by_name = True


class FeedbackResponse(BaseModel):
    """Response schema for feedback"""
    feedback_id: str = Field(..., alias="feedbackId")
    status: str = Field(default="recorded")
    processed_at: datetime = Field(..., alias="processedAt")
    
    class Config:
        populate_by_name = True


# ============== Health Schemas ==============

class ComponentHealth(BaseModel):
    """Health status of a component"""
    status: str
    latency_ms: Optional[float] = Field(None, alias="latencyMs")
    message: Optional[str] = None
    
    class Config:
        populate_by_name = True


class HealthResponse(BaseModel):
    """Response schema for health check"""
    status: str
    version: str
    timestamp: datetime
    components: Dict[str, ComponentHealth]
    
    class Config:
        populate_by_name = True


# ============== Model Management Schemas ==============

class ModelInfo(BaseModel):
    """Information about loaded model"""
    version: str
    path: str
    loaded_at: datetime = Field(..., alias="loadedAt")
    status: str
    
    class Config:
        populate_by_name = True


class ReloadModelResponse(BaseModel):
    """Response for model reload"""
    status: str
    previous_version: str = Field(..., alias="previousVersion")
    new_version: str = Field(..., alias="newVersion")
    reloaded_at: datetime = Field(..., alias="reloadedAt")
    
    class Config:
        populate_by_name = True
