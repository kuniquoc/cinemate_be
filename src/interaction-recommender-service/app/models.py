"""
SQLAlchemy database models for interaction events
"""
from datetime import datetime
from uuid import uuid4
from sqlalchemy import Column, String, DateTime, Text, Index, ForeignKey
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.orm import DeclarativeBase, relationship


class Base(DeclarativeBase):
    """SQLAlchemy declarative base"""
    pass


class InteractionEvent(Base):
    """
    Raw interaction events table
    Stores all user interactions with movies
    """
    __tablename__ = "interaction_events"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid4)
    request_id = Column(UUID(as_uuid=True), unique=True, nullable=False, default=uuid4)
    user_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    movie_id = Column(UUID(as_uuid=True), nullable=True, index=True)
    event_type = Column(String(50), nullable=False, index=True)
    event_data = Column(JSONB, nullable=True)
    client_timestamp = Column(DateTime(timezone=True), nullable=True)
    server_timestamp = Column(DateTime(timezone=True), nullable=False, default=datetime.utcnow)
    
    # Relationships
    audit_events = relationship("AuditEvent", back_populates="interaction_event")
    
    __table_args__ = (
        Index("idx_events_user_type", "user_id", "event_type"),
        Index("idx_events_movie_type", "movie_id", "event_type"),
        Index("idx_events_server_ts", "server_timestamp"),
    )


class UserFeatures(Base):
    """
    Materialized user features for recommendation
    Stores computed feature vectors per user
    """
    __tablename__ = "user_features"
    
    user_id = Column(UUID(as_uuid=True), primary_key=True)
    features = Column(JSONB, nullable=False, default=dict)
    version = Column(String(50), nullable=True)
    updated_at = Column(DateTime(timezone=True), nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)


class AuditEvent(Base):
    """
    Audit trail for processed events
    Tracks processing status and errors
    """
    __tablename__ = "audit_events"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid4)
    event_id = Column(UUID(as_uuid=True), ForeignKey("interaction_events.id"), nullable=False)
    status = Column(String(20), nullable=False)
    message = Column(Text, nullable=True)
    processed_at = Column(DateTime(timezone=True), nullable=False, default=datetime.utcnow)
    
    # Relationships
    interaction_event = relationship("InteractionEvent", back_populates="audit_events")
    
    __table_args__ = (
        Index("idx_audit_event_id", "event_id"),
        Index("idx_audit_status", "status"),
    )


class ModelFeedback(Base):
    """
    Model feedback for retraining
    Stores user feedback on recommendations
    """
    __tablename__ = "model_feedback"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid4)
    user_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    model_version = Column(String(100), nullable=False)
    impression_list = Column(JSONB, nullable=False)
    clicked_item_id = Column(UUID(as_uuid=True), nullable=True)
    watch_time_sec = Column(String(20), nullable=True)
    context = Column(String(50), nullable=True)
    feedback_timestamp = Column(DateTime(timezone=True), nullable=False)
    created_at = Column(DateTime(timezone=True), nullable=False, default=datetime.utcnow)
    
    __table_args__ = (
        Index("idx_feedback_user", "user_id"),
        Index("idx_feedback_model", "model_version"),
    )
