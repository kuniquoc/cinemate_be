"""
API Routes for feedback
"""
from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.kafka_client import kafka_manager
from app.feedback_service import FeedbackService
from app.schemas import FeedbackRequest, FeedbackResponse

router = APIRouter(tags=["Feedback"])


def get_feedback_service() -> FeedbackService:
    """Get feedback service instance"""
    return FeedbackService(kafka_manager)


@router.post("/feedback", response_model=FeedbackResponse, status_code=status.HTTP_200_OK)
async def submit_feedback(
    feedback: FeedbackRequest,
    session: AsyncSession = Depends(get_db),
    feedback_service: FeedbackService = Depends(get_feedback_service)
):
    """
    Submit user feedback on recommendations
    
    - **userId**: User identifier
    - **modelVersion**: Version of model that generated recommendations
    - **impressionList**: List of movie IDs that were shown
    - **clickedItemId**: Movie ID that user clicked (optional)
    - **watchTimeSec**: Watch time in seconds after clicking (optional)
    - **timestamp**: Feedback timestamp
    - **context**: Context where recommendations were shown (optional)
    
    This feedback is used to improve the recommendation model.
    """
    try:
        result = await feedback_service.process_feedback(
            user_id=feedback.user_id,
            model_version=feedback.model_version,
            impression_list=feedback.impression_list,
            clicked_item_id=feedback.clicked_item_id,
            watch_time_sec=feedback.watch_time_sec,
            timestamp=feedback.timestamp,
            context=feedback.context,
            session=session
        )
        
        return FeedbackResponse(
            feedbackId=result["feedbackId"],
            status=result["status"],
            processedAt=datetime.fromisoformat(result["processedAt"].replace("Z", "+00:00"))
        )
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to process feedback: {str(e)}"
        )
