from sqlalchemy import Column, Integer, String, DateTime, JSON, Float, Text
from sqlalchemy.sql import func
from sqlalchemy.dialects.postgresql import UUID
from app.database import Base


class Interaction(Base):
    __tablename__ = "interactions"

    id = Column(Integer, primary_key=True, index=True)
    event_type = Column(String(50), nullable=False)
    request_id = Column(String(128), nullable=True, index=True)
    user_id = Column(UUID(as_uuid=True), nullable=False, index=True)
    movie_id = Column(String(128), nullable=True, index=True)
    client_timestamp = Column(String(64), nullable=True)
    # 'metadata' is a reserved attribute name on Declarative Base; use
    # attribute `meta` while keeping the DB column named 'metadata'.
    meta = Column('metadata', JSON, nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class Movie(Base):
    __tablename__ = "movies"

    id = Column(Integer, primary_key=True, index=True)
    movie_id = Column(String(128), unique=True, index=True, nullable=False)
    title = Column(String(256), nullable=False)
    genres = Column(String(256), nullable=True)
    overview = Column(Text, nullable=True)
    actors = Column(String(512), nullable=True)
    directors = Column(String(512), nullable=True)


class UserFeatures(Base):
    __tablename__ = "user_features"
    # Use `user_id` as the primary key to match existing DB schema
    user_id = Column(UUID(as_uuid=True), primary_key=True, index=True, nullable=False)
    features = Column(JSON, nullable=False)
    version = Column(String(64), nullable=True)
    updated_at = Column(DateTime(timezone=True), default=func.now(), onupdate=func.now())


class Rating(Base):
    __tablename__ = "ratings"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(UUID(as_uuid=True), index=True, nullable=False)
    movie_id = Column(String(128), index=True, nullable=False)
    rating = Column(Float, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
