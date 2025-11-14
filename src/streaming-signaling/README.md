# Streaming Signaling Service

The streaming-signaling service coordinates peer discovery for movie playback via a WebSocket endpoint and Redis/Kafka backplane.

## Features

- WebSocket endpoint `/ws/signaling` supporting peer discovery and WebRTC signaling
- Message types: `whoHas`, `reportSegment`, `rtcOffer`, `rtcAnswer`, `iceCandidate`
- Redis-backed peer, segment, and metrics registries with TTL refresh
- Kafka subscriber per stream to fan-out playback events
- Actuator endpoints for health and metrics

## Protocol Documentation

See [Streaming Signaling Protocol](../../docs/streaming-signaling-protocol.md) for detailed message specifications.

## Message Types Overview

### Client → Server
- **whoHas**: Query which peers have a specific segment
- **reportSegment**: Report downloaded segment with metrics
- **rtcOffer**: Send WebRTC offer for P2P connection
- **rtcAnswer**: Send WebRTC answer for P2P connection
- **iceCandidate**: Exchange ICE candidates for NAT traversal

### Server → Client
- **peerList**: List of peers watching the same movie (sent on connect)
- **whoHasReply**: Response with peers that have the requested segment
- **reportAck**: Acknowledgment of segment report
- **error**: Error notification

## Configuration

Environment variables (defaults in `src/main/resources/application.yml`):

| Variable                                       | Description                                    |
| ---------------------------------------------- | ---------------------------------------------- |
| `SERVER_PORT`                                  | HTTP port (default `8083`).                    |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis connection.                              |
| `KAFKA_BOOTSTRAP_SERVERS`                      | Kafka brokers list (default `localhost:9092`). |
| `KAFKA_CONSUMER_GROUP`                         | Kafka consumer group id.                       |
| `STREAMING_TOPIC_PREFIX`                       | Prefix for per-stream Kafka topics.            |
| `STREAMING_MAX_ACTIVE_PEERS`                   | Max concurrent peer connections per viewer.    |

## Local Development

```bash
mvn -pl streaming-signaling -am spring-boot:run
```

Redis and Kafka must be reachable according to the configured environment variables.

## Testing

```bash
mvn -pl streaming-signaling test
```

## WebSocket Connection Example

```javascript
const ws = new WebSocket('ws://localhost:8083/ws/signaling?clientId=peer-123&streamId=movie-456');

ws.onopen = () => {
    // Send whoHas request
    ws.send(JSON.stringify({
        type: 'whoHas',
        movieId: 'movie-456',
        qualityId: '720p',
        segmentId: 'seg_0005'
    }));
};

ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    switch(message.type) {
        case 'peerList':
            console.log('Connected peers:', message.peers);
            break;
        case 'whoHasReply':
            console.log('Peers with segment:', message.peers);
            break;
        case 'reportAck':
            console.log('Report acknowledged:', message.segmentId);
            break;
        case 'error':
            console.error('Error:', message.message);
            break;
    }
};
```

