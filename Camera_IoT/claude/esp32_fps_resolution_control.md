# Điều chỉnh FPS & Resolution cho ESP32-CAM (MJPEG Stream)

Tài liệu mô tả cách chỉnh runtime độ phân giải (resolution), chất lượng ảnh (quality) và tốc độ khung hình (FPS) cho camera stream MJPEG trên ESP32-CAM, dùng thư viện `esp32-camera`.

## 1. Cấu hình khởi tạo camera

Phân nhánh theo việc board có PSRAM hay không — quyết định resolution/fb_count tối đa có thể dùng:

```cpp
if (psramFound()) {
    config.frame_size = FRAMESIZE_VGA;   // 640x480
    config.jpeg_quality = 10;            // 0-63, số nhỏ = chất lượng cao hơn
    config.fb_count = 2;                 // double buffer, cần PSRAM
    config.fb_location = CAMERA_FB_IN_PSRAM;
    config.grab_mode = CAMERA_GRAB_LATEST; // luôn lấy frame mới nhất, giảm lag
} else {
    config.frame_size = FRAMESIZE_QVGA;  // 320x240
    config.jpeg_quality = 12;
    config.fb_count = 1;                 // DRAM giới hạn, không đủ cho double buffer
    config.fb_location = CAMERA_FB_IN_DRAM;
}
```

**Lý do:** DRAM nội bộ ESP32 chỉ ~520KB, chia sẻ với WiFi/BT stack, nên không có PSRAM thì bắt buộc QVGA + single buffer. Có PSRAM mới đủ RAM chạy VGA trở lên với double buffer, giúp FPS mượt hơn (frame kế được chụp trong lúc frame trước đang gửi).

## 2. Giới hạn FPS trong stream handler

Thêm biến toàn cục `targetFPS` và chặn tốc độ lấy frame trong vòng lặp gửi MJPEG:

```cpp
volatile int targetFPS = 15; // chỉnh runtime qua /control?var=fps&val=xx

static esp_err_t stream_handler(httpd_req_t *req){
    camera_fb_t * fb = NULL;
    esp_err_t res = ESP_OK;
    size_t _jpg_buf_len = 0;
    uint8_t * _jpg_buf = NULL;
    char part_buf[64];
    unsigned long lastFrameTime = 0;

    res = httpd_resp_set_type(req, STREAM_CONTENT_TYPE);
    if(res != ESP_OK) return res;
    httpd_resp_set_hdr(req, "Access-Control-Allow-Origin", "*");

    while(true){
        unsigned long now = millis();
        unsigned long frameInterval = 1000 / targetFPS;
        if (now - lastFrameTime < frameInterval) {
            vTaskDelay(1); // nhường CPU, tránh busy-wait
            continue;
        }
        lastFrameTime = now;

        fb = esp_camera_fb_get();
        if (!fb) { res = ESP_FAIL; }
        else {
            if(fb->format != PIXFORMAT_JPEG){
                bool ok = frame2jpg(fb, 80, &_jpg_buf, &_jpg_buf_len);
                esp_camera_fb_return(fb); fb = NULL;
                if(!ok) res = ESP_FAIL;
            } else {
                _jpg_buf_len = fb->len;
                _jpg_buf = fb->buf;
            }
        }
        if(res == ESP_OK){
            size_t hlen = snprintf(part_buf, 64,
                "\r\n--frame\r\nContent-Type: image/jpeg\r\nContent-Length: %u\r\n\r\n", _jpg_buf_len);
            res = httpd_resp_send_chunk(req, part_buf, hlen);
        }
        if(res == ESP_OK) res = httpd_resp_send_chunk(req, (const char *)_jpg_buf, _jpg_buf_len);

        if(fb){ esp_camera_fb_return(fb); fb = NULL; _jpg_buf = NULL; }
        else if(_jpg_buf){ free(_jpg_buf); _jpg_buf = NULL; }

        if(res != ESP_OK) break;
    }
    return res;
}
```

## 3. Endpoint `/control` — chỉnh resolution/quality/fps runtime

Nếu đã có handler `/control` từ ví dụ CameraWebServer gốc, chỉ cần thêm nhánh `fps`. Nếu chưa có, dùng nguyên hàm dưới:

```cpp
static esp_err_t cmd_handler(httpd_req_t *req){
    char*  buf;
    size_t buf_len;
    char variable[32] = {0,};
    char value[32] = {0,};

    buf_len = httpd_req_get_url_query_len(req) + 1;
    if (buf_len > 1) {
        buf = (char*)malloc(buf_len);
        if(httpd_req_get_url_query_str(req, buf, buf_len) == ESP_OK) {
            if (httpd_query_key_value(buf, "var", variable, sizeof(variable)) != ESP_OK ||
                httpd_query_key_value(buf, "val", value, sizeof(value)) != ESP_OK) {
                free(buf);
                httpd_resp_send_404(req);
                return ESP_FAIL;
            }
        }
        free(buf);
    } else {
        httpd_resp_send_404(req);
        return ESP_FAIL;
    }

    int val = atoi(value);
    sensor_t * s = esp_camera_sensor_get();
    int res = 0;

    if(!strcmp(variable, "framesize")) {
        res = s->set_framesize(s, (framesize_t)val);
    } else if(!strcmp(variable, "quality")) {
        res = s->set_quality(s, val);
    } else if(!strcmp(variable, "fps")) {
        targetFPS = (val > 0) ? val : 1;
    }
    // có thể bổ sung thêm case: brightness, contrast, saturation, vflip, hmirror...

    if(res){
        return httpd_resp_send_500(req);
    }

    httpd_resp_set_hdr(req, "Access-Control-Allow-Origin", "*");
    return httpd_resp_send(req, NULL, 0);
}
```

Đăng ký route trong lúc khởi động HTTP server (`app_httpd_main()` hoặc tương đương):

```cpp
httpd_uri_t cmd_uri = {
    .uri       = "/control",
    .method    = HTTP_GET,
    .handler   = cmd_handler,
    .user_ctx  = NULL
};
httpd_register_uri_handler(camera_httpd, &cmd_uri);
```

## 4. Cách dùng (gọi từ trình duyệt hoặc app)

```
http://<esp32-ip>/control?var=framesize&val=8   # đổi resolution (giá trị enum framesize_t, kiểm tra sensor.h của bản SDK đang dùng)
http://<esp32-ip>/control?var=quality&val=12    # đổi jpeg quality (0-63)
http://<esp32-ip>/control?var=fps&val=10        # giới hạn FPS tối đa
```

Không cần build lại / nạp lại firmware khi đổi các giá trị này — áp dụng ngay trong lúc đang stream.

## 5. Lưu ý khi triển khai

- `targetFPS` chỉ là **giới hạn trên**. Nếu resolution/quality hiện tại khiến camera + WiFi không kịp tốc độ đó thì FPS thực tế sẽ thấp hơn, không cao hơn.
- Gọi `WiFi.setSleep(false);` sau khi kết nối WiFi — mặc định ESP32 bật modem sleep để tiết kiệm điện, gây độ trễ ngắt quãng khi stream.
- `CAMERA_GRAB_LATEST` cần `fb_count = 2` (chỉ khả dụng khi có PSRAM) — giúp tránh tình trạng frame bị dồn/kẹt trong hàng đợi khi tốc độ xử lý chậm hơn tốc độ chụp.
- Giá trị enum của `framesize_t` (QVGA, VGA, SVGA...) có thể khác thứ tự số giữa các phiên bản thư viện `esp32-camera` — nên kiểm tra `sensor.h` của bản đang dùng trước khi hard-code giá trị `val`.
