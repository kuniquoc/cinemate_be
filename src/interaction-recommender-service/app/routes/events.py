"""
API Routes for interaction events
"""
from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.cache import cache
from app.event_service import EventService
from app.schemas import (
    WatchEventRequest,
    SearchEventRequest,
    RatingEventRequest,
    FavoriteEventRequest,
    EventResponse
)

router = APIRouter(prefix="/api/v1/events", tags=["Events"])


def get_event_service() -> EventService:
    """Get event service instance"""
    return EventService(cache)


@router.post("/watch", response_model=EventResponse, status_code=status.HTTP_200_OK)
async def track_watch_event(
    event: WatchEventRequest,
    session: AsyncSession = Depends(get_db),
    event_service: EventService = Depends(get_event_service)
):
    """
    Track a watch event when user watches a movie
    
    - **requestId**: Optional unique request ID for idempotency
    - **userId**: User identifier
    - **movieId**: Movie identifier
    - **clientTimestamp**: Client-side timestamp
    - **metadata**: Watch metadata (duration, device, quality, etc.)
    """
    try:
        result = await event_service.process_event(
            event_type="watch",
            user_id=event.user_id,
            movie_id=event.movie_id,
            metadata=event.metadata.model_dump(by_alias=True),
            client_timestamp=event.client_timestamp,
            request_id=event.request_id,
            session=session
        )
        return EventResponse(
            requestId=result["requestId"],
            status=result["status"],
            serverTimestamp=datetime.fromisoformat(result["serverTimestamp"].replace("Z", "+00:00"))
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to process watch event: {str(e)}"
        )


@router.post("/search", response_model=EventResponse, status_code=status.HTTP_200_OK)
async def track_search_event(
    event: SearchEventRequest,
    session: AsyncSession = Depends(get_db),
    event_service: EventService = Depends(get_event_service)
):
    """
    Track a search event when user performs a search
    
    - **requestId**: Optional unique request ID for idempotency
    - **userId**: User identifier
    - **movieId**: Optional clicked movie from search results
    - **clientTimestamp**: Client-side timestamp
    - **metadata**: Search metadata (query, results count, filters)
    """
    try:
        result = await event_service.process_event(
            event_type="search",
            user_id=event.user_id,
            movie_id=event.movie_id,
            metadata=event.metadata.model_dump(by_alias=True),
            client_timestamp=event.client_timestamp,
            request_id=event.request_id,
            session=session
        )
        return EventResponse(
            requestId=result["requestId"],
            status=result["status"],
            serverTimestamp=datetime.fromisoformat(result["serverTimestamp"].replace("Z", "+00:00"))
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to process search event: {str(e)}"
        )


@router.post("/rating", response_model=EventResponse, status_code=status.HTTP_200_OK)
async def track_rating_event(
    event: RatingEventRequest,
    session: AsyncSession = Depends(get_db),
    event_service: EventService = Depends(get_event_service)
):
    """
    Track a rating event when user rates a movie
    
    - **requestId**: Optional unique request ID for idempotency
    - **userId**: User identifier
    - **movieId**: Movie identifier
    - **clientTimestamp**: Client-side timestamp
    - **metadata**: Rating metadata (rating value, previous rating)
    """
    try:
        result = await event_service.process_event(
            event_type="rating",
            user_id=event.user_id,
            movie_id=event.movie_id,
            metadata=event.metadata.model_dump(by_alias=True),
            client_timestamp=event.client_timestamp,
            request_id=event.request_id,
            session=session
        )
        return EventResponse(
            requestId=result["requestId"],
            status=result["status"],
            serverTimestamp=datetime.fromisoformat(result["serverTimestamp"].replace("Z", "+00:00"))
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to process rating event: {str(e)}"
        )


@router.post("/favorite", response_model=EventResponse, status_code=status.HTTP_200_OK)
async def track_favorite_event(
    event: FavoriteEventRequest,
    session: AsyncSession = Depends(get_db),
    event_service: EventService = Depends(get_event_service)
):
    """
    Track a favorite event when user adds/removes movie from favorites
    
    - **requestId**: Optional unique request ID for idempotency
    - **userId**: User identifier
    - **movieId**: Movie identifier
    - **clientTimestamp**: Client-side timestamp
    - **metadata**: Favorite metadata (action: add/remove)
    """
    try:
        result = await event_service.process_event(
            event_type="favorite",
            user_id=event.user_id,
            movie_id=event.movie_id,
            metadata=event.metadata.model_dump(by_alias=True),
            client_timestamp=event.client_timestamp,
            request_id=event.request_id,
            session=session
        )
        return EventResponse(
            requestId=result["requestId"],
            status=result["status"],
            serverTimestamp=datetime.fromisoformat(result["serverTimestamp"].replace("Z", "+00:00"))
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to process favorite event: {str(e)}"
        )
