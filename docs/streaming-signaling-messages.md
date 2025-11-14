# Streaming Signaling Messages - Quick Reference

## Segment ID Format

**Important:** Tất cả messages sử dụng Segment ID **BAO GỒM** file extension.

- **Format**: `seg_XXXX.m4s` (XXXX = zero-padded 4 digits)
- **Examples**: `seg_0000.m4s`, `seg_0005.m4s`, `seg_0142.m4s`
- **File trên disk**: `seg_0005.m4s` → **Segment ID**: `seg_0005.m4s`

```javascript
// Correct usage
const segmentId = "seg_0005.m4s";       // ✅
const filename = segmentId;              // segmentId chính là filename

// Wrong - don't strip extension
const segmentId = "seg_0005";           // ❌
```

---

## Message Types Summary

### Client → Server Messages

| Type            | Purpose                | Required Fields                     |
| --------------- | ---------------------- | ----------------------------------- |
| `whoHas`        | Tìm peer có segment    | `movieId`, `qualityId`, `segmentId` |
| `reportSegment` | Báo cáo đã tải segment | `qualityId`, `segmentId`            |
| `rtcOffer`      | WebRTC offer           | `to`, `sdp`                         |
| `rtcAnswer`     | WebRTC answer          | `to`, `sdp`                         |
| `iceCandidate`  | ICE candidate          | `to`, `candidate`                   |

### Server → Client Messages

| Type          | Purpose                 | Content                            |
| ------------- | ----------------------- | ---------------------------------- |
| `peerList`    | Danh sách peer đang xem | `streamId`, `peers[]`              |
| `whoHasReply` | Kết quả tìm peer        | `segmentId`, `peers[]` với metrics |
| `reportAck`   | Xác nhận báo cáo        | `segmentId`                        |
| `error`       | Thông báo lỗi           | `message`                          |

## Message Examples

### whoHas
```json
{
  "type": "whoHas",
  "movieId": "movie-123",
  "qualityId": "720p",
  "segmentId": "seg_0005.m4s"
}
```

**Note:** `segmentId` là tên segment **bao gồm extension** `.m4s`. Ví dụ: `seg_0005.m4s`, `seg_0142.m4s`. SegmentId chính là filename đầy đủ.

### whoHasReply
```json
{
  "type": "whoHasReply",
  "segmentId": "seg_0005.m4s",
  "peers": [{
    "peerId": "peer-abc",
    "metrics": {
      "uploadSpeed": 8.5,
      "latency": 150,
      "successRate": 0.98,
      "lastActive": 1731585600000
    }
  }]
}
```

### reportSegment
```json
{
  "type": "reportSegment",
  "qualityId": "720p",
  "segmentId": "seg_0005.m4s",
  "source": "peer",
  "latency": 250,
  "speed": 8.5
}
```

### reportAck
```json
{
  "type": "reportAck",
  "segmentId": "seg_0005.m4s"
}
```

### peerList
```json
{
  "type": "peerList",
  "streamId": "movie-123",
  "peers": ["peer-abc", "peer-def"]
}
```

### rtcOffer/rtcAnswer
```json
{
  "type": "rtcOffer",
  "to": "peer-456",
  "sdp": "v=0..."
}
```

### iceCandidate
```json
{
  "type": "iceCandidate",
  "to": "peer-456",
  "candidate": {
    "candidate": "...",
    "sdpMLineIndex": 0
  }
}
```

### error
```json
{
  "type": "error",
  "message": "Target peer is not connected"
}
```

## Common Error Messages

- `"Missing message type"` - Thiếu field `type`
- `"Unsupported message type: X"` - Type không hợp lệ
- `"whoHas requires movieId, qualityId and segmentId"` - Thiếu field bắt buộc
- `"reportSegment requires qualityId"` - Thiếu qualityId
- `"reportSegment requires segmentId"` - Thiếu segmentId
- `"RTC message requires 'to'"` - Thiếu peer đích
- `"Target peer is not connected: X"` - Peer không online

## Field Defaults

| Field                     | Default Value | When                    |
| ------------------------- | ------------- | ----------------------- |
| `movieId` (reportSegment) | from session  | Nếu không được cung cấp |
| `source` (reportSegment)  | `"peer"`      | Nếu không được cung cấp |
| `latency` (reportSegment) | `0`           | Nếu không hợp lệ        |
| `speed` (reportSegment)   | `0.0`         | Nếu không hợp lệ        |

## WebSocket Connection

**Endpoint:** `ws://<host>:<port>/ws/signaling`

**Query Params:**
- `clientId` (required): Unique peer identifier
- `streamId` (required): Movie ID

**Example:**
```
ws://localhost:8083/ws/signaling?clientId=peer-123&streamId=movie-456
```

## Typical Message Flow

1. **Connection** → Server sends `peerList`
2. Client sends `whoHas` → Server replies `whoHasReply`
3. Client sends `rtcOffer` → Server relays to peer
4. Peer sends `rtcAnswer` → Server relays back
5. Exchange `iceCandidate` messages
6. P2P connection established
7. Client sends `reportSegment` → Server replies `reportAck`

## See Also

- [Full Protocol Documentation](streaming-signaling-protocol.md)
- [Streaming Deployment Guide](streaming-deployment-guide.md)
