# Streaming Signaling Protocol

## Tổng quan

Streaming Signaling Service sử dụng WebSocket protocol để quản lý peer discovery và WebRTC signaling cho hệ thống P2P video streaming. Tất cả message được gửi dưới dạng JSON qua WebSocket connection.

## Kết nối WebSocket

### Endpoint
```
ws://<host>:<port>/ws/signaling
```

### Query Parameters
| Parameter  | Type   | Required | Description                                       |
| ---------- | ------ | -------- | ------------------------------------------------- |
| `clientId` | string | Yes      | Unique identifier của client/peer                 |
| `streamId` | string | Yes      | Movie ID mà client đang xem (cũng gọi là movieId) |

### Ví dụ kết nối
```javascript
const ws = new WebSocket('ws://localhost:8083/ws/signaling?clientId=peer-123&streamId=movie-456');
```

---

## Segment ID Format

Tất cả các message liên quan đến segment sử dụng **Segment ID** - một identifier bao gồm cả file extension.

### Format Specification
- **Pattern**: `seg_XXXX.m4s` 
- **XXXX**: Số thứ tự segment, zero-padded 4 digits (0000-9999)
- **Extension**: BAO GỒM `.m4s` extension trong segment ID

### Examples
| Segment File (trên disk) | Segment ID (trong messages) |
| ------------------------ | --------------------------- |
| `seg_0000.m4s`           | `seg_0000.m4s`              |
| `seg_0005.m4s`           | `seg_0005.m4s`              |
| `seg_0142.m4s`           | `seg_0142.m4s`              |
| `seg_9999.m4s`           | `seg_9999.m4s`              |

### Usage
```javascript
// ✅ CORRECT - Bao gồm extension, segmentId chính là filename
const segmentId = "seg_0005.m4s";

// ❌ WRONG - Không được strip extension
const segmentId = "seg_0005";

// SegmentId chính là filename, không cần build
const filename = segmentId; // "seg_0005.m4s"
```

### Trong HLS Playlist
```m3u8
#EXTM3U
#EXT-X-VERSION:7
#EXT-X-MAP:URI="init.mp4"
#EXTINF:7.407400,
seg_0000.m4s          ← Lấy luôn filename này làm segmentId
#EXTINF:5.271933,
seg_0001.m4s
```

Khi parse playlist để lấy segment IDs, giữ nguyên filename với extension:
```javascript
const filename = "seg_0005.m4s";
const segmentId = filename; // "seg_0005.m4s" - giữ nguyên
```

---

## Message Types

### 1. Client → Server Messages

#### 1.1. `whoHas` - Tìm kiếm peer có segment

**Mục đích:** Client yêu cầu server tìm các peer đang có một segment video cụ thể.

**Request Format:**
```json
{
  "type": "whoHas",
  "movieId": "3a044ac7-70c6-491f-9467-5eddc06d58b2",
  "qualityId": "720p",
  "segmentId": "segment-5.ts"
}
```

**Request Fields:**
| Field       | Type   | Required | Description                                                                          |
| ----------- | ------ | -------- | ------------------------------------------------------------------------------------ |
| `type`      | string | Yes      | Luôn là `"whoHas"`                                                                   |
| `movieId`   | string | Yes      | ID của movie đang xem                                                                |
| `qualityId` | string | Yes      | Quality level (ví dụ: `"360p"`, `"720p"`, `"1080p"`)                                 |
| `segmentId` | string | Yes      | Segment identifier **bao gồm extension** (ví dụ: `"seg_0005.m4s"`, `"seg_0142.m4s"`) |

**Segment ID Format:**
- Format: `seg_XXXX.m4s` với XXXX là số thứ tự segment (zero-padded 4 digits)
- **BAO GỒM** extension `.m4s`
- Ví dụ: `seg_0000.m4s`, `seg_0001.m4s`, `seg_0142.m4s`
- SegmentId chính là filename đầy đủ, không cần build thêm

**Validation Rules:**
- Tất cả các field đều không được null, empty hoặc chỉ chứa whitespace
- Server sẽ trả về error message nếu thiếu bất kỳ field nào

**Response:** Server sẽ trả về message `whoHasReply` (xem section 2.1)

---

#### 1.2. `reportSegment` - Báo cáo đã tải segment

**Mục đích:** Client thông báo với server rằng đã hoàn thành việc tải xuống một segment, kèm metrics về performance.
**Request Format:**
```json
{
  "type": "reportSegment",
  "movieId": "3a044ac7-70c6-491f-9467-5eddc06d58b2",
  "qualityId": "720p",
  "segmentId": "seg_0005.m4s",
  "source": "peer",
  "latency": 250,
  "speed": 5.2
}
```

**Request Fields:**
| Field       | Type   | Required | Default      | Description                                              |
| ----------- | ------ | -------- | ------------ | -------------------------------------------------------- |
| `type`      | string | Yes      | -            | Luôn là `"reportSegment"`                                |
| `movieId`   | string | No       | from session | ID của movie (có thể lấy từ connection query param)      |
| `qualityId` | string | Yes      | -            | Quality level của segment                                |
| `segmentId` | string | Yes      | -            | Segment filename (ví dụ: `"seg_0005.m4s"`)               |
| `source`    | string | No       | `"peer"`     | Nguồn tải: `"peer"` (từ P2P) hoặc `"server"` (từ origin) |
| `latency`   | number | No       | `0`          | Thời gian tải (milliseconds)                             |
| `speed`     | number | No       | `0.0`        | Tốc độ tải (Mbps)                                        |

**Validation Rules:**
- `qualityId` và `segmentId` bắt buộc và không được empty
- `movieId` có thể bỏ qua, server sẽ lấy từ session attribute
- `source` mặc định là `"peer"` nếu không được cung cấp
- `latency` và `speed` sẽ mặc định là 0 nếu không hợp lệ

**Response:** Server sẽ trả về message `reportAck` (xem section 2.2)

**Use Case:**
- Client tải segment từ peer khác qua P2P → báo cáo với `source: "peer"`
- Client tải segment từ CDN/origin server → báo cáo với `source: "server"`
- Metrics được sử dụng để tính toán peer quality và ưu tiên peer selection

---

#### 1.3. `rtcOffer` - WebRTC Offer

**Mục đích:** Gửi WebRTC offer để khởi tạo P2P connection với peer khác.

**Request Format:**
```json
{
  "type": "rtcOffer",
  "from": "peer-123",
  "to": "peer-456",
  "streamId": "movie-789",
  "sdp": "v=0\r\no=- ... (WebRTC SDP)"
}
```

**Request Fields:**
| Field      | Type   | Required | Description                              |
| ---------- | ------ | -------- | ---------------------------------------- |
| `type`     | string | Yes      | Luôn là `"rtcOffer"`                     |
| `from`     | string | Auto-set | ClientId của sender (server tự động set) |
| `to`       | string | Yes      | ClientId của peer đích                   |
| `streamId` | string | Yes      | Movie ID                                 |
| `sdp`      | string | Yes      | WebRTC Session Description Protocol      |

**Validation Rules:**
- `to` field bắt buộc và không được blank
- Server sẽ kiểm tra target peer có đang kết nối không
- Server tự động override `from` field bằng clientId từ session

**Behavior:**
- Server relay message này tới peer được chỉ định trong `to` field
- Nếu peer không tồn tại hoặc không connected, server trả về error

---

#### 1.4. `rtcAnswer` - WebRTC Answer

**Mục đích:** Trả lời WebRTC offer để hoàn thành P2P connection setup.

**Request Format:**
```json
{
  "type": "rtcAnswer",
  "from": "peer-456",
  "to": "peer-123",
  "streamId": "movie-789",
  "sdp": "v=0\r\no=- ... (WebRTC SDP)"
}
```

**Request Fields:**
| Field      | Type   | Required | Description                              |
| ---------- | ------ | -------- | ---------------------------------------- |
| `type`     | string | Yes      | Luôn là `"rtcAnswer"`                    |
| `from`     | string | Auto-set | ClientId của sender (server tự động set) |
| `to`       | string | Yes      | ClientId của peer đích                   |
| `streamId` | string | Yes      | Movie ID                                 |
| `sdp`      | string | Yes      | WebRTC Session Description Protocol      |

**Validation Rules:**
- Giống như `rtcOffer`

**Behavior:**
- Server relay message này tới peer được chỉ định
- Hoàn thành negotiation cho WebRTC connection

---

#### 1.5. `iceCandidate` - ICE Candidate

**Mục đích:** Trao đổi ICE candidates để thiết lập NAT traversal cho P2P connection.

**Request Format:**
```json
{
  "type": "iceCandidate",
  "from": "peer-123",
  "to": "peer-456",
  "streamId": "movie-789",
  "candidate": {
    "candidate": "candidate:... typ host ...",
    "sdpMLineIndex": 0,
    "sdpMid": "0"
  }
}
```

**Request Fields:**
| Field       | Type   | Required | Description                    |
| ----------- | ------ | -------- | ------------------------------ |
| `type`      | string | Yes      | Luôn là `"iceCandidate"`       |
| `from`      | string | Auto-set | ClientId của sender            |
| `to`        | string | Yes      | ClientId của peer đích         |
| `streamId`  | string | Yes      | Movie ID                       |
| `candidate` | object | Yes      | ICE candidate object từ WebRTC |

**Validation Rules:**
- `to` field bắt buộc
- Server relay trực tiếp tới target peer

**Behavior:**
- Server hoạt động như một signaling relay
- Không validate nội dung của candidate object

---

### 2. Server → Client Messages

#### 2.1. `peerList` - Danh sách peer

**Mục đích:** Server gửi danh sách tất cả các peer đang xem cùng movie khi client mới kết nối.

**Response Format:**
```json
{
  "type": "peerList",
  "streamId": "3a044ac7-70c6-491f-9467-5eddc06d58b2",
  "peers": [
    "peer-abc",
    "peer-def",
    "peer-xyz"
  ]
}
```

**Response Fields:**
| Field      | Type          | Description                                                           |
| ---------- | ------------- | --------------------------------------------------------------------- |
| `type`     | string        | Luôn là `"peerList"`                                                  |
| `streamId` | string        | Movie ID mà các peer đang xem                                         |
| `peers`    | array[string] | Danh sách clientId của các peer khác (không bao gồm chính client này) |

**When Sent:**
- Ngay sau khi WebSocket connection được established
- Tự động gửi mà không cần request

**Use Case:**
- Client dùng danh sách này để biết có bao nhiêu peer đang xem cùng video
- Có thể dùng để khởi tạo WebRTC connections với các peer

---

#### 2.2. `whoHasReply` - Kết quả tìm kiếm peer

**Mục đích:** Trả về danh sách các peer đang có segment được yêu cầu, kèm metrics.

**Response Format:**
```json
{
  "type": "whoHasReply",
  "segmentId": "seg_0005",
  "peers": [
    {
      "peerId": "peer-abc",
      "metrics": {
        "uploadSpeed": 8.5,
        "latency": 150,
        "successRate": 0.98,
        "lastActive": 1731585600000
      }
    },
    {
      "peerId": "peer-def",
      "metrics": {
        "uploadSpeed": 12.3,
        "latency": 80,
        "successRate": 1.0,
        "lastActive": 1731585590000
      }
    }
  ]
}
```

**Response Fields:**
| Field                         | Type          | Description                                                |
| ----------------------------- | ------------- | ---------------------------------------------------------- |
| `type`                        | string        | Luôn là `"whoHasReply"`                                    |
| `segmentId`                   | string        | Segment ID được yêu cầu trong `whoHas` message             |
| `peers`                       | array[object] | Danh sách peer có segment (có thể rỗng nếu không tìm thấy) |
| `peers[].peerId`              | string        | ClientId của peer                                          |
| `peers[].metrics`             | object        | Performance metrics của peer                               |
| `peers[].metrics.uploadSpeed` | number        | Tốc độ upload trung bình (Mbps)                            |
| `peers[].metrics.latency`     | number        | Độ trễ trung bình (milliseconds)                           |
| `peers[].metrics.successRate` | number        | Tỷ lệ thành công (0.0 - 1.0)                               |
| `peers[].metrics.lastActive`  | number        | Timestamp lần cuối active (Unix epoch milliseconds)        |

**Use Case:**
- Client chọn peer tốt nhất dựa trên metrics (ví dụ: uploadSpeed cao, latency thấp)
- Client có thể fallback về server nếu `peers` array rỗng
- Metrics được cập nhật dựa trên các `reportSegment` messages trước đó

---

#### 2.3. `reportAck` - Xác nhận báo cáo

**Mục đích:** Xác nhận server đã nhận và xử lý `reportSegment` message.

**Response Format:**
```json
{
  "type": "reportAck",
  "segmentId": "seg_0005"
}
```

**Response Fields:**
| Field       | Type   | Description                |
| ----------- | ------ | -------------------------- |
| `type`      | string | Luôn là `"reportAck"`      |
| `segmentId` | string | Segment ID đã được báo cáo |

**Use Case:**
- Confirmation cho client biết metrics đã được ghi nhận
- Client có thể track số segment đã báo cáo thành công

---

#### 2.4. `error` - Thông báo lỗi

**Mục đích:** Thông báo cho client khi có lỗi xảy ra.

**Response Format:**
```json
{
  "type": "error",
  "message": "whoHas requires movieId, qualityId and segmentId"
}
```

**Response Fields:**
| Field     | Type   | Description        |
| --------- | ------ | ------------------ |
| `type`    | string | Luôn là `"error"`  |
| `message` | string | Mô tả lỗi chi tiết |

**Common Error Messages:**
| Error Message                                        | Cause                                              |
| ---------------------------------------------------- | -------------------------------------------------- |
| `"Missing message type"`                             | Request không có `type` field hoặc `type` là empty |
| `"Unsupported message type: <type>"`                 | `type` không thuộc các giá trị hợp lệ              |
| `"whoHas requires movieId, qualityId and segmentId"` | Request `whoHas` thiếu field bắt buộc              |
| `"reportSegment requires qualityId"`                 | Request `reportSegment` thiếu `qualityId`          |
| `"reportSegment requires segmentId"`                 | Request `reportSegment` thiếu `segmentId`          |
| `"RTC message requires 'to'"`                        | WebRTC message thiếu `to` field                    |
| `"Target peer is not connected: <peerId>"`           | Peer đích không online                             |

---

## Message Flow Examples

### Example 1: Client tìm kiếm và tải segment từ peer

```
Client                          Server                          Peer
  |                               |                               |
  |-- (1) whoHas ---------------->|                               |
  |   {                           |                               |
  |     type: "whoHas",           |                               |
  |     movieId: "movie-1",       |                               |
  |     qualityId: "720p",        |                               |
  |     segmentId: "seg_0005"     |                               |
  |   }                           |                               |
  |                               |                               |
  |<- (2) whoHasReply ------------|                               |
  |   {                           |                               |
  |     type: "whoHasReply",      |                               |
  |     segmentId: "seg_0005",    |                               |
  |     peers: [{peerId: "peer-abc", metrics: {...}}]             |
  |   }                           |                               |
  |                               |                               |
  |-- (3) rtcOffer --------------->|                               |
  |                               |-- (4) rtcOffer -------------->|
  |                               |                               |
  |                               |<- (5) rtcAnswer --------------|
  |<- (6) rtcAnswer --------------|                               |
  |                               |                               |
  |<=========== (7) P2P Data Channel Connection ================>|
  |                               |                               |
  |-- (8) reportSegment --------->|                               |
  |   {                           |                               |
  |     type: "reportSegment",    |                               |
  |     qualityId: "720p",        |                               |
  |     segmentId: "seg_0005",    |                               |
  |     source: "peer",           |                               |
  |     latency: 250,             |                               |
  |     speed: 8.5                |                               |
  |   }                           |                               |
  |                               |                               |
  |<- (9) reportAck --------------|                               |
  |   {                           |                               |
  |     type: "reportAck",        |                               |
  |     segmentId: "seg_0005"     |                               |
  |   }                           |                               |
```

### Example 2: Client kết nối lần đầu

```
Client                          Server
  |                               |
  |-- WebSocket Connect --------->|
  |   ?clientId=peer-123          |
  |   &streamId=movie-456         |
  |                               |
  |<- Connection Established -----|
  |                               |
  |<- (1) peerList ---------------|
  |   {                           |
  |     type: "peerList",         |
  |     streamId: "movie-456",    |
  |     peers: ["peer-aaa",       |
  |              "peer-bbb",       |
  |              "peer-ccc"]       |
  |   }                           |
```

---

## Data Models

### PeerInfo
```java
public record PeerInfo(
    String peerId,
    PeerMetrics metrics
)
```

### PeerMetrics
```java
public record PeerMetrics(
    double uploadSpeed,    // Mbps
    int latency,          // milliseconds
    double successRate,   // 0.0 - 1.0
    long lastActive       // Unix epoch milliseconds
)
```

---

## Best Practices

### Client Implementation

1. **Connection Management**
   - Implement reconnection logic với exponential backoff
   - Handle connection close events gracefully
   - Validate server responses

2. **Peer Selection**
   - Ưu tiên peer có `uploadSpeed` cao và `latency` thấp
   - Tính `successRate` vào quyết định chọn peer
   - Có fallback mechanism khi không tìm thấy peer

3. **Metrics Reporting**
   - Luôn report segment sau khi tải xong
   - Tính toán chính xác `latency` và `speed`
   - Báo cáo đúng `source` (peer hoặc server)

4. **Error Handling**
   - Catch và log tất cả error messages
   - Retry logic cho failed requests
   - Timeout cho `whoHas` requests

### Server Configuration

1. **Redis TTL**: Cấu hình TTL phù hợp cho peer và segment data
2. **Kafka Topics**: Sử dụng topic riêng cho mỗi stream/movie
3. **Max Peers**: Giới hạn số peer connections đồng thời

---

## WebSocket Events

### Connection Events

| Event     | When                   | Action                       |
| --------- | ---------------------- | ---------------------------- |
| `open`    | Connection established | Server gửi `peerList`        |
| `message` | Message received       | Parse và route theo `type`   |
| `close`   | Connection closed      | Server cleanup peer registry |
| `error`   | Connection error       | Client should reconnect      |

---

## Version History

| Version | Date       | Changes                                     |
| ------- | ---------- | ------------------------------------------- |
| 1.0.0   | 2025-11-14 | Initial version với camelCase message types |

---

## Notes

- Tất cả message types đã được chuyển từ UPPER_CASE sang camelCase để nhất quán
- Server tự động validate và sanitize tất cả input (trim whitespace)
- WebRTC messages được relay trực tiếp giữa các peer
- Metrics được lưu trong Redis với TTL để tự động cleanup
