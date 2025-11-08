# Thư mục dữ liệu kiểm thử

Đặt các video mẫu phục vụ cho việc chạy bộ mô phỏng Movie Service.

## Các file nên có

### `sample-video.mp4`
- Video nhỏ (< 100MB) dùng cho upload trực tiếp.
- Có thể dùng bất kỳ định dạng phổ biến: MP4, AVI, MOV, MKV, WebM.
- Gợi ý nguồn:
  - Tự quay nhanh bằng điện thoại.
  - Tải từ https://sample-videos.com/.
  - Tạo bằng ffmpeg: `ffmpeg -f lavfi -i testsrc=duration=30:size=640x480:rate=30 sample-video.mp4`.

### `large-video.mp4` (khuyến khích)
- Video lớn (> 100MB) để kiểm thử upload theo chunk.
- Có thể dùng chung định dạng với file nhỏ, chỉ cần dung lượng lớn hơn.

## Tạo video mẫu bằng FFmpeg

```bash
# Video 30s (~5MB)
ffmpeg -f lavfi -i testsrc=duration=30:size=640x480:rate=30 sample-video.mp4

# Video 5 phút (~50MB)
ffmpeg -f lavfi -i testsrc=duration=300:size=1280x720:rate=30 large-video.mp4

# Video 10 phút FullHD (~200MB)
ffmpeg -f lavfi -i testsrc=duration=600:size=1920x1080:rate=30 large-video.mp4
```

## Quy tắc đặt tên

- Giữ nguyên tên `sample-video.mp4` và `large-video.mp4` để khớp cấu hình mặc định.
- Nếu sử dụng tên khác, sửa lại đường dẫn tương ứng trong `../config/config.js`.

## Định dạng hỗ trợ

- MP4 (khuyến nghị)
- AVI
- MOV
- MKV
- WebM

> Repository không kèm theo video để giảm dung lượng. Hãy tự chuẩn bị dữ liệu phù hợp với nhu cầu test.