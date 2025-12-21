from datetime import datetime
from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field


class MetadataModel(BaseModel):
    pass


class WatchMetadata(MetadataModel):
    watchDuration: Optional[int]
    device: Optional[str]
    quality: Optional[str]
    sessionId: Optional[str]
    progressPercent: Optional[float]


class WatchEventRequest(BaseModel):
    request_id: Optional[str] = Field(None, alias="requestId")
    user_id: str = Field(..., alias="userId")
    movie_id: str = Field(..., alias="movieId")
    client_timestamp: Optional[str] = Field(None, alias="clientTimestamp")
    metadata: WatchMetadata


class SearchMetadata(MetadataModel):
    query: str
    resultsCount: Optional[int]
    filters: Optional[Dict[str, Any]]


class SearchEventRequest(BaseModel):
    request_id: Optional[str] = Field(None, alias="requestId")
    user_id: str = Field(..., alias="userId")
    movie_id: Optional[str] = Field(None, alias="movieId")
    client_timestamp: Optional[str] = Field(None, alias="clientTimestamp")
    metadata: SearchMetadata


class RatingMetadata(MetadataModel):
    rating: float
    previousRating: Optional[float]


class RatingEventRequest(BaseModel):
    request_id: Optional[str] = Field(None, alias="requestId")
    user_id: str = Field(..., alias="userId")
    movie_id: str = Field(..., alias="movieId")
    client_timestamp: Optional[str] = Field(None, alias="clientTimestamp")
    metadata: RatingMetadata


class FavoriteMetadata(MetadataModel):
    action: str


class FavoriteEventRequest(BaseModel):
    request_id: Optional[str] = Field(None, alias="requestId")
    user_id: str = Field(..., alias="userId")
    movie_id: str = Field(..., alias="movieId")
    client_timestamp: Optional[str] = Field(None, alias="clientTimestamp")
    metadata: FavoriteMetadata


class EventResponse(BaseModel):
    requestId: Optional[str]
    status: str
    serverTimestamp: datetime


class RecommendationItem(BaseModel):
    movieId: str
    score: float
    reasons: Optional[List[str]] = []


class RecommendationResponse(BaseModel):
    userId: str
    modelVersion: str
    cached: bool
    recommendations: List[RecommendationItem]
    generatedAt: datetime
    context: Optional[str]


class UserFeaturesResponse(BaseModel):
    userId: str
    features: Dict[str, Any]
    version: Optional[str]
    updatedAt: datetime
