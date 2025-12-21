from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker, declarative_base
from contextlib import asynccontextmanager
from app.config import get_settings

settings = get_settings()

ENGINE = create_async_engine(settings.database_url, future=True, echo=False)
AsyncSessionLocal = sessionmaker(ENGINE, class_=AsyncSession, expire_on_commit=False)
Base = declarative_base()


async def init_db():
    # Create tables if not exist
    async with ENGINE.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)


async def close_db():
    await ENGINE.dispose()


async def get_db():
    async with AsyncSessionLocal() as session:
        yield session


@asynccontextmanager
async def get_db_session():
    async with AsyncSessionLocal() as session:
        yield session
