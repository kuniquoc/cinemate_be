"""
Feedback service for model feedback collection
"""
import logging
from datetime import datetime
from typing import Dict, Any
from uuid import uuid4

from sqlalchemy.ext.asyncio import AsyncSession

from app.models import ModelFeedback
from app.kafka_client import KafkaManager
from app.config import get_settings

logger = logging.getLogger(__name__)

settings = get_settings()


class FeedbackService:
    """
    Service for collecting and processing model feedback
    Used for model improvement and A/B testing analysis
    """
    
    def __init__(self, kafka: KafkaManager):
        self.kafka = kafka
    
    async def process_feedback(
        self,
        user_id: str,
        model_version: str,
        impression_list: list,
        clicked_item_id: str | None,
        watch_time_sec: int | None,
        timestamp: datetime,
        context: str | None,
        session: AsyncSession
    ) -> Dict[str, Any]:
        """
        Process user feedback on recommendations
        
        Args:
            user_id: User identifier
            model_version: Model version that generated recommendations
            impression_list: List of movie IDs shown to user
            clicked_item_id: Movie ID that user clicked (if any)
            watch_time_sec: Watch time after clicking (if applicable)
            timestamp: Feedback timestamp
            context: Context of recommendation display
            session: Database session
            
        Returns:
            Feedback processing result
        """
        feedback_id = str(uuid4())
        processed_at = datetime.utcnow()
        
        try:
            # Store feedback in database
            feedback = ModelFeedback(
                id=feedback_id,
                user_id=user_id,
                model_version=model_version,
                impression_list=impression_list,
                clicked_item_id=clicked_item_id,
                watch_time_sec=str(watch_time_sec) if watch_time_sec else None,
                context=context,
                feedback_timestamp=timestamp,
                created_at=processed_at
            )
            session.add(feedback)
            await session.flush()
            
            # Prepare Kafka message
            kafka_message = {
                "feedbackId": feedback_id,
                "userId": user_id,
                "modelVersion": model_version,
                "impressionList": impression_list,
                "clickedItemId": clicked_item_id,
                "watchTimeSec": watch_time_sec,
                "context": context,
                "feedbackTimestamp": timestamp.isoformat() + "Z",
                "processedAt": processed_at.isoformat() + "Z",
                # Computed metrics
                "metrics": self._compute_feedback_metrics(
                    impression_list, clicked_item_id, watch_time_sec
                )
            }
            
            # Publish to Kafka for offline analysis
            await self.kafka.publish_feedback(kafka_message)
            
            await session.commit()
            
            logger.info(f"Feedback processed: {feedback_id}")
            
            return {
                "feedbackId": feedback_id,
                "status": "recorded",
                "processedAt": processed_at.isoformat() + "Z"
            }
            
        except Exception as e:
            await session.rollback()
            logger.error(f"Error processing feedback: {e}")
            raise
    
    def _compute_feedback_metrics(
        self,
        impression_list: list,
        clicked_item_id: str | None,
        watch_time_sec: int | None
    ) -> Dict[str, Any]:
        """Compute feedback metrics for analysis"""
        metrics = {
            "impressionCount": len(impression_list),
            "hasClick": clicked_item_id is not None,
            "hasWatch": watch_time_sec is not None and watch_time_sec > 0,
        }
        
        if clicked_item_id and clicked_item_id in impression_list:
            metrics["clickPosition"] = impression_list.index(clicked_item_id) + 1
            metrics["clickRank"] = metrics["clickPosition"]
        
        if watch_time_sec:
            metrics["watchTimeSec"] = watch_time_sec
            # Engagement score based on watch time
            if watch_time_sec > 600:  # > 10 min
                metrics["engagementLevel"] = "high"
            elif watch_time_sec > 180:  # > 3 min
                metrics["engagementLevel"] = "medium"
            else:
                metrics["engagementLevel"] = "low"
        
        return metrics
