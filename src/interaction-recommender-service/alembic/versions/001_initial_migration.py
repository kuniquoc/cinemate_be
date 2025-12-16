"""Initial migration

Revision ID: 001
Revises: 
Create Date: 2025-01-01 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = '001'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Create interaction_events table
    op.create_table(
        'interaction_events',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('request_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('movie_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('event_type', sa.String(50), nullable=False),
        sa.Column('event_data', postgresql.JSONB(astext_type=sa.Text()), nullable=True),
        sa.Column('client_timestamp', sa.DateTime(timezone=True), nullable=True),
        sa.Column('server_timestamp', sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('request_id')
    )
    op.create_index('idx_events_user', 'interaction_events', ['user_id'])
    op.create_index('idx_events_movie', 'interaction_events', ['movie_id'])
    op.create_index('idx_events_type', 'interaction_events', ['event_type'])
    op.create_index('idx_events_user_type', 'interaction_events', ['user_id', 'event_type'])
    op.create_index('idx_events_movie_type', 'interaction_events', ['movie_id', 'event_type'])
    op.create_index('idx_events_server_ts', 'interaction_events', ['server_timestamp'])

    # Create user_features table
    op.create_table(
        'user_features',
        sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('features', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('version', sa.String(50), nullable=True),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint('user_id')
    )

    # Create audit_events table
    op.create_table(
        'audit_events',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('event_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('status', sa.String(20), nullable=False),
        sa.Column('message', sa.Text(), nullable=True),
        sa.Column('processed_at', sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(['event_id'], ['interaction_events.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('idx_audit_event_id', 'audit_events', ['event_id'])
    op.create_index('idx_audit_status', 'audit_events', ['status'])

    # Create model_feedback table
    op.create_table(
        'model_feedback',
        sa.Column('id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('model_version', sa.String(100), nullable=False),
        sa.Column('impression_list', postgresql.JSONB(astext_type=sa.Text()), nullable=False),
        sa.Column('clicked_item_id', postgresql.UUID(as_uuid=True), nullable=True),
        sa.Column('watch_time_sec', sa.String(20), nullable=True),
        sa.Column('context', sa.String(50), nullable=True),
        sa.Column('feedback_timestamp', sa.DateTime(timezone=True), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index('idx_feedback_user', 'model_feedback', ['user_id'])
    op.create_index('idx_feedback_model', 'model_feedback', ['model_version'])


def downgrade() -> None:
    op.drop_table('model_feedback')
    op.drop_table('audit_events')
    op.drop_table('user_features')
    op.drop_table('interaction_events')
