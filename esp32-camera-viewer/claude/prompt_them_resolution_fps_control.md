# Prompt: Thêm chức năng chỉnh Resolution & FPS cho "ESP32 Camera Viewer"

> Dùng file này paste trực tiếp vào AI coding assistant trong IntelliJ (AI Assistant / Junie / Copilot Chat...) để nó implement giúp bạn.

---

## Bối cảnh

Tôi có một ứng dụng desktop tên **"ESP32 Camera Viewer"** (project đang mở trong IntelliJ) dùng để xem stream MJPEG từ camera ESP32-CAM. Giao diện hiện tại có: label tiêu đề, khung hiển thị hình ảnh, label "FPS: 0", label "Status: Disconnected", và 2 nút **Start** / **Stop**.

Phía firmware ESP32 đã có sẵn HTTP endpoint để điều khiển camera runtime, không cần reset thiết bị:

```
GET http://<esp32-ip>/control?var=framesize&val=<int>   # đổi độ phân giải
GET http://<esp32-ip>/control?var=quality&val=<int>     # đổi jpeg quality (0-63, số nhỏ = chất lượng cao)
GET http://<esp32-ip>/control?var=fps&val=<int>         # giới hạn FPS tối đa (1-30)
```

Trả về HTTP 200 nếu áp dụng thành công, HTTP 500 nếu set thất bại (giá trị không hợp lệ).

Bảng mapping `val` cho `framesize` (theo enum `framesize_t` của thư viện `esp32-camera` — **verify lại với `sensor.h` của firmware thực tế trước khi hard-code**, vì thứ tự có thể lệch giữa các version):

| val | Tên enum         | Độ phân giải |
|-----|------------------|--------------|
| 0   | FRAMESIZE_96X96  | 96x96        |
| 1   | FRAMESIZE_QQVGA  | 160x120      |
| 2   | FRAMESIZE_QCIF   | 176x144      |
| 3   | FRAMESIZE_HQVGA  | 240x176      |
| 4   | FRAMESIZE_240X240| 240x240      |
| 5   | FRAMESIZE_QVGA   | 320x240      |
| 6   | FRAMESIZE_CIF    | 400x296      |
| 7   | FRAMESIZE_HVGA   | 480x320      |
| 8   | FRAMESIZE_VGA    | 640x480      |
| 9   | FRAMESIZE_SVGA   | 800x600      |
| 10  | FRAMESIZE_XGA    | 1024x768     |
| 11  | FRAMESIZE_HD     | 1280x720     |
| 12  | FRAMESIZE_SXGA   | 1280x1024    |
| 13  | FRAMESIZE_UXGA   | 1600x1200    |

## Yêu cầu

Trước khi code, **hãy đọc code hiện tại của project** (tìm class chứa UI chính — nơi có nút Start/Stop, label FPS/Status, và logic đang đọc MJPEG stream/HTTP) để biết đang dùng framework gì (Swing, JavaFX, hay Compose Desktop) và cách đang gọi HTTP tới ESP32 (host/IP đang lưu ở đâu), rồi áp dụng đúng convention/style đang có trong project. Không tạo file/class trùng lặp logic đã có sẵn.

Sau đó thêm các chức năng sau:

1. **UI mới** — thêm 1 khu vực "Camera Settings" (panel/section riêng, đặt phía trên hoặc dưới khu vực Start/Stop):
   - Dropdown/ComboBox **Resolution**, liệt kê các option dễ hiểu cho người dùng (ví dụ "QVGA (320x240)", "VGA (640x480)", "SVGA (800x600)", "XGA (1024x768)"...) — không hiện số `val` thô, map ẩn phía trong code theo bảng trên.
   - Spinner hoặc Slider **Target FPS**, khoảng giá trị 1–30, mặc định 15.
   - Slider hoặc Spinner **Quality** (0–63, mặc định giữ giá trị hiện có của firmware, ví dụ 10-12), có thể optional nếu muốn giữ đơn giản trước.
   - Nút **Apply** để gửi thay đổi (không cần tự động apply mỗi lần kéo slider, tránh spam request).

2. **Logic gọi API**:
   - Khi bấm Apply, gửi các HTTP GET tương ứng tới `/control` (dùng lại HTTP client/base URL đã có trong project, không hardcode IP mới nếu đã có field nhập IP/host).
   - Gọi **bất đồng bộ** (SwingWorker / CompletableFuture / coroutine tùy framework đang dùng) — không được block UI thread khi chờ response.
   - Hiển thị kết quả ngắn gọn trong khu vực Status: ví dụ "Status: Applied VGA @ 15fps" khi thành công, hoặc "Status: Failed to apply settings" khi lỗi (timeout, HTTP 500, mất kết nối...).
   - Nếu đang stream (trạng thái Connected), việc đổi cài đặt phải áp dụng ngay lập tức mà không cần bấm Stop/Start lại — vì endpoint ESP32 đã hỗ trợ đổi runtime.

3. **Phân biệt rõ 2 khái niệm FPS**:
   - "FPS" hiện tại trên UI là FPS *thực đo được* của stream (đo bằng cách đếm số frame nhận được mỗi giây ở phía client) — giữ nguyên logic này.
   - "Target FPS" là giá trị người dùng đặt để giới hạn tốc độ gửi từ ESP32 — chỉ là giá trị mong muốn gửi lên thiết bị, không phải số đo. Đừng nhầm lẫn 2 label này trong UI (đặt tên rõ ràng, ví dụ "FPS (live): X" và "Target FPS: Y").

4. **(Tuỳ chọn, làm nếu không tốn nhiều thời gian)** Lưu lựa chọn resolution/fps/quality gần nhất (dùng `java.util.prefs.Preferences` hoặc cơ chế settings đã có sẵn trong project) để lần mở app sau tự điền lại giá trị cũ, không bắt người dùng chọn lại từ đầu.

5. **Validate input**: không cho gửi request nếu FPS/Quality ngoài khoảng hợp lệ; disable nút Apply hoặc hiện cảnh báo nếu chưa Connect tới ESP32 (không có IP hợp lệ).

## Kiểm thử sau khi implement

- Chạy app, connect tới ESP32, đổi resolution → xác nhận khung hình trong viewer đổi kích thước tương ứng.
- Đổi target FPS thấp (ví dụ 5) → xác nhận label FPS (live) giảm theo, không còn chạy full tốc độ cũ.
- Ngắt kết nối ESP32 giữa chừng rồi bấm Apply → xác nhận app không crash, hiện thông báo lỗi hợp lý.
- Đổi resolution/fps liên tục vài lần → xác nhận không có memory leak / thread bị treo (kiểm tra UI vẫn responsive).
