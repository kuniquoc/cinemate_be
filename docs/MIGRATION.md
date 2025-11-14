# Message Type Migration: UPPER_CASE → camelCase

## Tóm tắt thay đổi

Tất cả message types trong Streaming Signaling Protocol đã được chuyển từ UPPER_CASE sang camelCase để đồng nhất và dễ sử dụng hơn.

## Bảng chuyển đổi

| Loại                  | Cũ (UPPER_CASE)  | Mới (camelCase) | Thay đổi |
| --------------------- | ---------------- | --------------- | -------- |
| **Client → Server**   |
| Peer Discovery        | `WHO_HAS`        | `whoHas`        | ✓        |
| Segment Report        | `REPORT_SEGMENT` | `reportSegment` | ✓        |
| WebRTC Offer          | `RTC_OFFER`      | `rtcOffer`      | ✓        |
| WebRTC Answer         | `RTC_ANSWER`     | `rtcAnswer`     | ✓        |
| ICE Candidate         | `ICE_CANDIDATE`  | `iceCandidate`  | ✓        |
| **Server → Client**   |
| Peer List             | `peer_list`      | `peerList`      | ✓        |
| Peer Discovery Reply  | `WHO_HAS_REPLY`  | `whoHasReply`   | ✓        |
| Report Acknowledgment | `REPORT_ACK`     | `reportAck`     | ✓        |
| Error Message         | `ERROR`          | `error`         | ✓        |

## Files đã được cập nhật

### Backend (Java)

| File                                                                                                                   | Mô tả             | Thay đổi                        |
| ---------------------------------------------------------------------------------------------------------------------- | ----------------- | ------------------------------- |
| `src/streaming-signaling/src/main/java/com/pbl6/cinemate/streaming/signaling/dto/ErrorMessage.java`                    | Error DTO         | `ERROR` → `error`               |
| `src/streaming-signaling/src/main/java/com/pbl6/cinemate/streaming/signaling/dto/PeerListMessage.java`                 | Peer list DTO     | `peer_list` → `peerList`        |
| `src/streaming-signaling/src/main/java/com/pbl6/cinemate/streaming/signaling/dto/WhoHasReplyMessage.java`              | WhoHas reply DTO  | `WHO_HAS_REPLY` → `whoHasReply` |
| `src/streaming-signaling/src/main/java/com/pbl6/cinemate/streaming/signaling/dto/ReportSegmentAckMessage.java`         | Report ack DTO    | `REPORT_ACK` → `reportAck`      |
| `src/streaming-signaling/src/main/java/com/pbl6/cinemate/streaming/signaling/websocket/StreamingWebSocketHandler.java` | WebSocket handler | Switch cases và error messages  |

### Frontend (JavaScript)

| File                                                              | Mô tả             | Thay đổi             |
| ----------------------------------------------------------------- | ----------------- | -------------------- |
| `client-simulator/scripts/streaming/signaling-client.js`          | Signaling client  | Tất cả message types |
| `client-simulator/scripts/streaming/webrtc/connection-manager.js` | WebRTC connection | Event handlers       |
| `client-simulator/scripts/streaming/viewer.js`                    | Video viewer      | Error messages       |

### Documentation

| File                                   | Mô tả                                          |
| -------------------------------------- | ---------------------------------------------- |
| `docs/streaming-signaling-protocol.md` | **MỚI** - Tài liệu chi tiết đầy đủ về protocol |
| `docs/streaming-signaling-messages.md` | **MỚI** - Quick reference cho messages         |
| `src/streaming-signaling/README.md`    | Updated - Thêm message overview và examples    |
| `docs/MIGRATION.md`                    | **File này** - Tổng hợp thay đổi               |

## Chi tiết thay đổi từng message

### 1. whoHas (Client → Server)

**Trước:**
```json
{ "type": "WHO_HAS", "movieId": "...", "qualityId": "...", "segmentId": "..." }
```

**Sau:**
```json
{ "type": "whoHas", "movieId": "...", "qualityId": "...", "segmentId": "..." }
```

**Impact:**
- Java: Switch case trong `StreamingWebSocketHandler`
- JavaScript: Request payload trong `SignalingClient.requestWhoHas()`

---

### 2. whoHasReply (Server → Client)

**Trước:**
```json
{ "type": "WHO_HAS_REPLY", "segmentId": "...", "peers": [...] }
```

**Sau:**
```json
{ "type": "whoHasReply", "segmentId": "...", "peers": [...] }
```

**Impact:**
- Java: Constructor default value trong `WhoHasReplyMessage`
- JavaScript: Switch case trong `SignalingClient.handleMessage()`

---

### 3. reportSegment (Client → Server)

**Trước:**
```json
{ "type": "REPORT_SEGMENT", "qualityId": "...", "segmentId": "...", ... }
```

**Sau:**
```json
{ "type": "reportSegment", "qualityId": "...", "segmentId": "...", ... }
```

**Impact:**
- Java: Switch case và error messages
- JavaScript: Request payload trong `SignalingClient.reportSegment()`

---

### 4. reportAck (Server → Client)

**Trước:**
```json
{ "type": "REPORT_ACK", "segmentId": "..." }
```

**Sau:**
```json
{ "type": "reportAck", "segmentId": "..." }
```

**Impact:**
- Java: Constructor default value trong `ReportSegmentAckMessage`
- JavaScript: Switch case (emit 'report_ack' event không đổi)

---

### 5. peerList (Server → Client)

**Trước:**
```json
{ "type": "peer_list", "streamId": "...", "peers": [...] }
```

**Sau:**
```json
{ "type": "peerList", "streamId": "...", "peers": [...] }
```

**Impact:**
- Java: Constructor default value trong `PeerListMessage`
- JavaScript: Switch case (emit 'peer_list' event không đổi)

---

### 6. rtcOffer, rtcAnswer, iceCandidate (WebRTC Signaling)

**Trước:**
```json
{ "type": "RTC_OFFER", "to": "...", "sdp": "..." }
{ "type": "RTC_ANSWER", "to": "...", "sdp": "..." }
{ "type": "ICE_CANDIDATE", "to": "...", "candidate": {...} }
```

**Sau:**
```json
{ "type": "rtcOffer", "to": "...", "sdp": "..." }
{ "type": "rtcAnswer", "to": "...", "sdp": "..." }
{ "type": "iceCandidate", "to": "...", "candidate": {...} }
```

**Impact:**
- Java: Switch case trong `StreamingWebSocketHandler.handleRtcRelay()`
- JavaScript: 
  - Send methods: `sendRtcOffer()`, `sendRtcAnswer()`, `sendIceCandidate()`
  - Event listeners trong `WebRtcConnectionManager`

---

### 7. error (Server → Client)

**Trước:**
```json
{ "type": "ERROR", "message": "..." }
```

**Sau:**
```json
{ "type": "error", "message": "..." }
```

**Impact:**
- Java: Constructor default value trong `ErrorMessage`
- JavaScript: Switch case (backward compatible - xử lý cả 'ERROR' và 'error')

---

## Backward Compatibility

### JavaScript Client
JavaScript client đã được cập nhật để xử lý cả hai format (cũ và mới) cho error messages:

```javascript
case 'ERROR':  // Old format (deprecated)
case 'error':  // New format
    this.log.error(`Signaling error: ${message.message || 'unknown error'}`);
    break;
```

### Recommended Migration Path

1. **Deploy Backend**: Deploy streaming-signaling service với camelCase messages
2. **Update Clients**: Cập nhật client-simulator và các client khác để sử dụng camelCase
3. **Testing**: Test kỹ toàn bộ flow (peer discovery, reporting, WebRTC)
4. **Remove Compatibility**: Sau khi tất cả clients đã update, có thể bỏ backward compatibility cho 'ERROR'

## Breaking Changes

⚠️ **CẢNH BÁO**: Đây là breaking change. Tất cả clients phải được cập nhật để tương thích với message types mới.

### Checklist cho Migration

- [x] Update Java backend DTOs
- [x] Update Java backend message handlers
- [x] Update JavaScript signaling client
- [x] Update JavaScript WebRTC connection manager
- [x] Update JavaScript viewer
- [x] Create comprehensive documentation
- [ ] Deploy backend service
- [ ] Update all client applications
- [ ] Test end-to-end flow
- [ ] Monitor for errors

## Testing Checklist

### Unit Tests
- [ ] Test message serialization/deserialization
- [ ] Test WebSocket handler routing
- [ ] Test error message generation

### Integration Tests
- [ ] Test client connection and peerList reception
- [ ] Test whoHas → whoHasReply flow
- [ ] Test reportSegment → reportAck flow
- [ ] Test WebRTC signaling relay (offer/answer/ICE)
- [ ] Test error handling

### End-to-End Tests
- [ ] Test full playback flow with multiple peers
- [ ] Test peer-to-peer segment transfer
- [ ] Test fallback to server when no peers available
- [ ] Test reconnection scenarios

## References

- [Streaming Signaling Protocol Documentation](streaming-signaling-protocol.md)
- [Message Quick Reference](streaming-signaling-messages.md)
- [Streaming Deployment Guide](streaming-deployment-guide.md)

## Change Log

| Date       | Version | Author | Changes                        |
| ---------- | ------- | ------ | ------------------------------ |
| 2025-11-14 | 1.0.0   | System | Initial migration to camelCase |

## Support

For questions or issues related to this migration, please refer to:
1. Full protocol documentation: `docs/streaming-signaling-protocol.md`
2. Quick reference: `docs/streaming-signaling-messages.md`
3. Service README: `src/streaming-signaling/README.md`
