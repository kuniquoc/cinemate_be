"""
Event service for handling interaction events
"""
import logging
from datetime import datetime
from typing import Dict, Any, Optional
from uuid import uuid4

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.models import InteractionEvent, AuditEvent
from app.cache import RedisCache
from app.kafka_client import KafkaManager
from app.config import get_settings

logger = logging.getLogger(__name__)

settings = get_settings()


class EventService:
    """
    Service for processing and storing interaction events
    Handles idempotency, persistence, and Kafka publishing
    """
    
    def __init__(self, cache: RedisCache, kafka: KafkaManager):
        self.cache = cache
        self.kafka = kafka
    
    async def process_event(
        self,
        event_type: str,
        user_id: str,
        movie_id: Optional[str],
        metadata: Dict[str, Any],
        client_timestamp: Optional[datetime],
        request_id: Optional[str],
        session: AsyncSession
    ) -> Dict[str, Any]:
        """
        Process an interaction event
        
        Args:
            event_type: Type of event (watch, search, rating, favorite)
            user_id: User identifier
            movie_id: Movie identifier (optional for search)
            metadata: Event-specific metadata
            client_timestamp: Client-provided timestamp
            request_id: Request ID for idempotency
            session: Database session
            
        Returns:
            Response with request ID and status
        """
        # Generate request ID if not provided
        req_id = request_id or str(uuid4())
        server_ts = datetime.utcnow()
        
        # Check idempotency
        if await self.cache.check_request_processed(req_id):
            logger.info(f"Duplicate request detected: {req_id}")
            return {
                "requestId": req_id,
                "status": "duplicate",
                "serverTimestamp": server_ts.isoformat() + "Z"
            }
        
        try:
            # Persist to database
            event = InteractionEvent(
                request_id=req_id,
                user_id=user_id,
                movie_id=movie_id,
                event_type=event_type.lower(),
                event_data=metadata,
                client_timestamp=client_timestamp,
                server_timestamp=server_ts
            )
            session.add(event)
            await session.flush()  # Get event ID
            
            # Mark request as processed (idempotency)
            await self.cache.mark_request_processed(req_id)
            
            # Prepare Kafka message
            kafka_message = {
                "requestId": req_id,
                "eventId": str(event.id),
                "userId": user_id,
                "movieId": movie_id,
                "eventType": event_type.upper(),
                "clientTimestamp": client_timestamp.isoformat() + "Z" if client_timestamp else None,
                "serverTimestamp": server_ts.isoformat() + "Z",
                "metadata": metadata
            }
            
            # Publish to Kafka (async, non-blocking)
            published = await self.kafka.publish_interaction_event(kafka_message)
            
            # Create audit entry
            audit = AuditEvent(
                event_id=event.id,
                status="published" if published else "pending",
                message="Event published to Kafka" if published else "Kafka publish pending"
            )
            session.add(audit)
            
            await session.commit()
            
            logger.info(f"Event processed: {req_id} ({event_type})")
            
            return {
                "requestId": req_id,
                "status": "accepted",
                "serverTimestamp": server_ts.isoformat() + "Z"
            }
            
        except Exception as e:
            await session.rollback()
            logger.error(f"Error processing event {req_id}: {e}")
            raise
    
    async def get_user_events(
        self,
        user_id: str,
        event_type: Optional[str],
        limit: int,
        session: AsyncSession
    ) -> list:
        """Get user events from database"""
        query = select(InteractionEvent).where(
            InteractionEvent.user_id == user_id
        )
        
        if event_type:
            query = query.where(InteractionEvent.event_type == event_type.lower())
        
        query = query.order_by(InteractionEvent.server_timestamp.desc()).limit(limit)
        
        result = await session.execute(query)
        events = result.scalars().all()
        
        return [
            {
                "id": str(e.id),
                "requestId": str(e.request_id),
                "movieId": str(e.movie_id) if e.movie_id else None,
                "eventType": e.event_type,
                "eventData": e.event_data,
                "clientTimestamp": e.client_timestamp.isoformat() if e.client_timestamp else None,
                "serverTimestamp": e.server_timestamp.isoformat()
            }
            for e in events
        ]
