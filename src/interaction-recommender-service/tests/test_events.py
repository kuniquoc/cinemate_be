"""
Tests for event endpoints
"""
import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_health_endpoint(client: AsyncClient):
    """Test health endpoint"""
    response = await client.get("/health/live")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "alive"


@pytest.mark.asyncio
async def test_root_endpoint(client: AsyncClient):
    """Test root endpoint"""
    response = await client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "service" in data
    assert "version" in data


@pytest.mark.asyncio
async def test_watch_event(client: AsyncClient):
    """Test watch event tracking"""
    event_data = {
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "movieId": "550e8400-e29b-41d4-a716-446655440001",
        "clientTimestamp": "2025-01-01T12:00:00Z",
        "metadata": {
            "watchDuration": 3600,
            "device": "web",
            "quality": "1080p"
        }
    }
    
    response = await client.post("/api/v1/events/watch", json=event_data)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "accepted"
    assert "requestId" in data


@pytest.mark.asyncio
async def test_search_event(client: AsyncClient):
    """Test search event tracking"""
    event_data = {
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "clientTimestamp": "2025-01-01T12:00:00Z",
        "metadata": {
            "query": "action movies",
            "resultsCount": 25
        }
    }
    
    response = await client.post("/api/v1/events/search", json=event_data)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "accepted"


@pytest.mark.asyncio
async def test_rating_event(client: AsyncClient):
    """Test rating event tracking"""
    event_data = {
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "movieId": "550e8400-e29b-41d4-a716-446655440001",
        "clientTimestamp": "2025-01-01T12:00:00Z",
        "metadata": {
            "rating": 4.5
        }
    }
    
    response = await client.post("/api/v1/events/rating", json=event_data)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "accepted"


@pytest.mark.asyncio
async def test_favorite_event(client: AsyncClient):
    """Test favorite event tracking"""
    event_data = {
        "userId": "550e8400-e29b-41d4-a716-446655440000",
        "movieId": "550e8400-e29b-41d4-a716-446655440001",
        "clientTimestamp": "2025-01-01T12:00:00Z",
        "metadata": {
            "action": "add"
        }
    }
    
    response = await client.post("/api/v1/events/favorite", json=event_data)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "accepted"


@pytest.mark.asyncio
async def test_recommendations(client: AsyncClient):
    """Test recommendations endpoint"""
    user_id = "550e8400-e29b-41d4-a716-446655440000"
    
    response = await client.get(f"/recommend/{user_id}?k=10")
    assert response.status_code == 200
    data = response.json()
    assert data["userId"] == user_id
    assert "recommendations" in data
    assert len(data["recommendations"]) <= 10
