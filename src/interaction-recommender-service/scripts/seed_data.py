"""
Seed data script for interaction-recommender-service.
Run with: python scripts/seed_data.py

This script seeds initial data for development/testing purposes.
Only runs when SEED_ENABLED=true environment variable is set
and the tables are empty.
"""

import os
import sys
from datetime import datetime, timedelta, timezone
from uuid import UUID
import random

# Add the app directory to the path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker

from app.models import Base, InteractionEvent, UserFeatures, AuditEvent, ModelFeedback
from app.config import get_settings


# ==================== UUID Constants ====================
# These UUIDs match the Java SeedUUIDs.java for cross-service consistency

class SeedUUIDs:
    """Fixed UUIDs for seed data consistency across services."""
    
    class Users:
        ADMIN_01 = UUID("c0000000-0000-0000-0000-000000000001")
        ADMIN_02 = UUID("c0000000-0000-0000-0000-000000000002")
        USER_01 = UUID("c0000000-0000-0000-0000-000000000003")
        USER_02 = UUID("c0000000-0000-0000-0000-000000000004")
        USER_03 = UUID("c0000000-0000-0000-0000-000000000005")
        USER_04 = UUID("c0000000-0000-0000-0000-000000000006")
        USER_05 = UUID("c0000000-0000-0000-0000-000000000007")
        USER_06 = UUID("c0000000-0000-0000-0000-000000000008")
        USER_07 = UUID("c0000000-0000-0000-0000-000000000009")
        USER_08 = UUID("c0000000-0000-0000-0000-000000000010")
        USER_09 = UUID("c0000000-0000-0000-0000-000000000011")
        USER_10 = UUID("c0000000-0000-0000-0000-000000000012")
    
    class Movies:
        MOVIE_01 = UUID("f2000000-0000-0000-0000-000000000001")
        MOVIE_02 = UUID("f2000000-0000-0000-0000-000000000002")
        MOVIE_03 = UUID("f2000000-0000-0000-0000-000000000003")
        MOVIE_04 = UUID("f2000000-0000-0000-0000-000000000004")
        MOVIE_05 = UUID("f2000000-0000-0000-0000-000000000005")
        MOVIE_06 = UUID("f2000000-0000-0000-0000-000000000006")
        MOVIE_07 = UUID("f2000000-0000-0000-0000-000000000007")
        MOVIE_08 = UUID("f2000000-0000-0000-0000-000000000008")
        MOVIE_09 = UUID("f2000000-0000-0000-0000-000000000009")
        MOVIE_10 = UUID("f2000000-0000-0000-0000-000000000010")
        MOVIE_11 = UUID("f2000000-0000-0000-0000-000000000011")
        MOVIE_12 = UUID("f2000000-0000-0000-0000-000000000012")


# Event types for interactions
EVENT_TYPES = ["watch", "search", "rating", "favorite", "view", "click"]


def get_database_url():
    """Get database URL from settings or environment."""
    try:
        settings = get_settings()
        return settings.database_url
    except Exception:
        # Fallback to environment variable (no hardcoded password for security)
        db_url = os.getenv("DATABASE_URL")
        if not db_url:
            raise ValueError(
                "DATABASE_URL environment variable is required. "
                "Example: DATABASE_URL=postgresql://user:password@localhost:5436/interaction_db"
            )
        return db_url


def is_seed_enabled():
    """Check if seeding is enabled via environment variable."""
    return os.getenv("SEED_ENABLED", "false").lower() == "true"


def seed_interaction_events(session):
    """Seed interaction events table."""
    count = session.query(InteractionEvent).count()
    if count > 0:
        print(f"InteractionEvents table has {count} records. Skipping seeding.")
        return
    
    print("Seeding interaction events...")
    
    users = [
        SeedUUIDs.Users.USER_01, SeedUUIDs.Users.USER_02, SeedUUIDs.Users.USER_03,
        SeedUUIDs.Users.USER_04, SeedUUIDs.Users.USER_05, SeedUUIDs.Users.USER_06,
        SeedUUIDs.Users.USER_07, SeedUUIDs.Users.USER_08, SeedUUIDs.Users.USER_09,
        SeedUUIDs.Users.USER_10
    ]
    
    movies = [
        SeedUUIDs.Movies.MOVIE_01, SeedUUIDs.Movies.MOVIE_02, SeedUUIDs.Movies.MOVIE_03,
        SeedUUIDs.Movies.MOVIE_04, SeedUUIDs.Movies.MOVIE_05, SeedUUIDs.Movies.MOVIE_06,
        SeedUUIDs.Movies.MOVIE_07, SeedUUIDs.Movies.MOVIE_08, SeedUUIDs.Movies.MOVIE_09,
        SeedUUIDs.Movies.MOVIE_10, SeedUUIDs.Movies.MOVIE_11, SeedUUIDs.Movies.MOVIE_12
    ]
    
    events = []
    now = datetime.now(timezone.utc)
    
    for i in range(20):
        user_id = users[i % len(users)]
        movie_id = movies[i % len(movies)]
        event_type = EVENT_TYPES[i % len(EVENT_TYPES)]
        
        # Create event data based on type
        if event_type == "watch":
            event_data = {
                "duration_seconds": random.randint(600, 7200),
                "progress_percent": random.randint(10, 100),
                "quality": random.choice(["720p", "1080p", "4K"])
            }
        elif event_type == "rating":
            event_data = {
                "stars": random.randint(1, 5),
                "review": "Great movie!" if random.random() > 0.5 else None
            }
        elif event_type == "search":
            event_data = {
                "query": random.choice(["action", "comedy", "thriller", "drama"]),
                "results_count": random.randint(5, 50)
            }
        elif event_type == "favorite":
            event_data = {
                "action": random.choice(["add", "remove"])
            }
        else:
            event_data = {}
        
        event = InteractionEvent(
            user_id=user_id,
            movie_id=movie_id,
            event_type=event_type,
            event_data=event_data,
            client_timestamp=now - timedelta(days=random.randint(0, 30)),
            server_timestamp=now - timedelta(days=random.randint(0, 30))
        )
        events.append(event)
    
    session.add_all(events)
    session.commit()
    print(f"Successfully seeded {len(events)} interaction events.")


def seed_user_features(session):
    """Seed user features table."""
    count = session.query(UserFeatures).count()
    if count > 0:
        print(f"UserFeatures table has {count} records. Skipping seeding.")
        return
    
    print("Seeding user features...")
    
    users = [
        SeedUUIDs.Users.USER_01, SeedUUIDs.Users.USER_02, SeedUUIDs.Users.USER_03,
        SeedUUIDs.Users.USER_04, SeedUUIDs.Users.USER_05, SeedUUIDs.Users.USER_06,
        SeedUUIDs.Users.USER_07, SeedUUIDs.Users.USER_08, SeedUUIDs.Users.USER_09,
        SeedUUIDs.Users.USER_10
    ]
    
    features_list = []
    now = datetime.now(timezone.utc)
    
    for user_id in users:
        features = {
            "preferred_genres": random.sample(
                ["action", "comedy", "drama", "horror", "sci-fi", "romance", "thriller"],
                k=random.randint(2, 4)
            ),
            "avg_watch_time": random.randint(30, 120),
            "total_watches": random.randint(5, 50),
            "favorite_count": random.randint(0, 20),
            "avg_rating_given": round(random.uniform(3.0, 5.0), 2),
            "last_activity_days_ago": random.randint(0, 14)
        }
        
        user_feature = UserFeatures(
            user_id=user_id,
            features=features,
            version="v1.0.0",
            updated_at=now
        )
        features_list.append(user_feature)
    
    session.add_all(features_list)
    session.commit()
    print(f"Successfully seeded {len(features_list)} user features.")


def seed_model_feedback(session):
    """Seed model feedback table."""
    count = session.query(ModelFeedback).count()
    if count > 0:
        print(f"ModelFeedback table has {count} records. Skipping seeding.")
        return
    
    print("Seeding model feedback...")
    
    users = [
        SeedUUIDs.Users.USER_01, SeedUUIDs.Users.USER_02, SeedUUIDs.Users.USER_03,
        SeedUUIDs.Users.USER_04, SeedUUIDs.Users.USER_05
    ]
    
    movies = [
        SeedUUIDs.Movies.MOVIE_01, SeedUUIDs.Movies.MOVIE_02, SeedUUIDs.Movies.MOVIE_03,
        SeedUUIDs.Movies.MOVIE_04, SeedUUIDs.Movies.MOVIE_05, SeedUUIDs.Movies.MOVIE_06
    ]
    
    feedback_list = []
    now = datetime.now(timezone.utc)
    
    for i, user_id in enumerate(users):
        # Create impression list (recommended movies)
        impression_list = [
            {"movie_id": str(movies[j]), "position": j + 1, "score": round(random.uniform(0.5, 1.0), 3)}
            for j in range(min(5, len(movies)))
        ]
        
        # User may have clicked on one of the recommendations
        clicked_item = movies[random.randint(0, len(movies) - 1)] if random.random() > 0.3 else None
        
        feedback = ModelFeedback(
            user_id=user_id,
            model_version="recommendation-v1.0.0",
            impression_list=impression_list,
            clicked_item_id=clicked_item,
            watch_time_sec=str(random.randint(300, 3600)) if clicked_item else None,
            context=random.choice(["home", "search", "category", "similar"]),
            feedback_timestamp=now - timedelta(hours=random.randint(1, 72)),
            created_at=now
        )
        feedback_list.append(feedback)
    
    session.add_all(feedback_list)
    session.commit()
    print(f"Successfully seeded {len(feedback_list)} model feedback records.")


def seed_audit_events(session):
    """Seed audit events for existing interaction events."""
    count = session.query(AuditEvent).count()
    if count > 0:
        print(f"AuditEvents table has {count} records. Skipping seeding.")
        return
    
    print("Seeding audit events...")
    
    # Get all interaction events
    events = session.query(InteractionEvent).limit(10).all()
    if not events:
        print("No interaction events found. Skipping audit event seeding.")
        return
    
    audit_events = []
    now = datetime.now(timezone.utc)
    
    statuses = ["PROCESSED", "PROCESSED", "PROCESSED", "FAILED", "PENDING"]
    
    for i, event in enumerate(events):
        status = statuses[i % len(statuses)]
        message = None
        if status == "FAILED":
            message = "Processing failed due to invalid data format"
        elif status == "PROCESSED":
            message = "Successfully processed and features updated"
        
        audit = AuditEvent(
            event_id=event.id,
            status=status,
            message=message,
            processed_at=now - timedelta(minutes=random.randint(1, 60))
        )
        audit_events.append(audit)
    
    session.add_all(audit_events)
    session.commit()
    print(f"Successfully seeded {len(audit_events)} audit events.")


def main():
    """Main function to run all seeders."""
    if not is_seed_enabled():
        print("Seeding is disabled. Set SEED_ENABLED=true to enable.")
        print("Example: SEED_ENABLED=true python scripts/seed_data.py")
        return
    
    print("Starting interaction-recommender-service data seeding...")
    print(f"Database URL: {get_database_url()[:50]}...")
    
    try:
        # Create database connection
        engine = create_engine(get_database_url())
        Session = sessionmaker(bind=engine)
        session = Session()
        
        # Run seeders
        seed_interaction_events(session)
        seed_user_features(session)
        seed_model_feedback(session)
        seed_audit_events(session)
        
        session.close()
        print("\nData seeding completed successfully!")
        
    except Exception as e:
        print(f"\nError during seeding: {e}")
        raise


if __name__ == "__main__":
    main()
