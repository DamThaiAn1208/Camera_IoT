# Setup Mosquitto + Avahi (mDNS) trên Raspberry Pi

Tài liệu này chỉ áp dụng cho **bên Raspberry Pi** (không phải code Java hay firmware ESP32).
Mục tiêu: ESP32 tìm được IP của Pi qua mDNS thay vì hardcode IP, rồi connect vào Mosquitto chạy trên Pi.

---

## 1. Cài Mosquitto broker

```bash
sudo apt update
sudo apt install -y mosquitto mosquitto-clients
sudo systemctl enable mosquitto
sudo systemctl status mosquitto
```

Mặc định Mosquitto listen ở `0.0.0.0:1883`, cho phép anonymous connect — đủ dùng để test trong mạng nhà. Nếu muốn siết lại (khuyến khích khi ra khỏi giai đoạn test), sửa `/etc/mosquitto/conf.d/local.conf`:

```conf
listener 1883
allow_anonymous false
password_file /etc/mosquitto/passwd
```

```bash
sudo mosquitto_passwd -c /etc/mosquitto/passwd esp32cam
sudo systemctl restart mosquitto
```

---

## 2. Kiểm tra Avahi đã chạy (thường có sẵn)

```bash
systemctl status avahi-daemon
```

Nếu chưa có (hiếm khi xảy ra trên Raspberry Pi OS mặc định):

```bash
sudo apt install -y avahi-daemon
sudo systemctl enable --now avahi-daemon
```

Kiểm tra hostname mDNS hiện tại (hostname Pi này đang là `raspberry`, tức mDNS name là `raspberry.local` — đổi được qua `sudo raspi-config` > System Options > Hostname):

```bash
hostname
avahi-resolve -n raspberry.local
```

---

## 3. Quảng bá MQTT qua Avahi

Avahi mặc định **không** tự biết Mosquitto đang chạy — phải khai báo thủ công 1 file service.

```bash
sudo cp mqtt.service /etc/avahi/services/mqtt.service
sudo systemctl restart avahi-daemon
```

(File `mqtt.service` nằm cùng thư mục với README này, nội dung khai báo `_mqtt._tcp` port `1883`.)

---

## 4. Kiểm tra quảng bá hoạt động

Từ **một máy khác** trong cùng mạng WiFi (laptop, không phải chính con Pi):

```bash
avahi-browse -r _mqtt._tcp
```

Kỳ vọng thấy output tương tự:

```
+   wlan0 IPv4 MQTT Broker on raspberry        _mqtt._tcp           local
   hostname = [raspberry.local]
   address = [192.168.x.x]
   port = [1883]
```

Nếu không cài `avahi-browse` được (Windows/Mac không có sẵn công cụ này), có thể test nhanh bằng cách chỉ resolve hostname:

```bash
ping raspberry.local
```

---

## 5. Test publish/subscribe thủ công (không cần code)

Terminal 1 (subscribe):
```bash
mosquitto_sub -h localhost -t "camera/#" -v
```

Terminal 2 (publish, từ Pi hoặc máy khác cùng mạng, thay `<pi-ip>`):
```bash
mosquitto_pub -h <pi-ip> -t "camera/esp32_001/status" -m "online"
```

Nếu terminal 1 nhận được message → Mosquitto hoạt động đúng, sẵn sàng để ESP32 kết nối vào.

---

## 6. Việc còn lại ở phía ESP32 (chưa làm trong bước này)

- Thêm thư viện `ESPmDNS.h` + 1 MQTT client library (khuyến nghị `PubSubClient` hoặc `espMqttClient`).
- Boot xong WiFi → `MDNS.queryHost("raspberry")` để lấy IP → connect Mosquitto bằng IP đó (không dùng chuỗi `.local` trực tiếp).
- Có retry (2-3 lần) nếu query mDNS lần đầu miss ngay sau khi WiFi vừa connect.

Phần này sẽ làm ở firmware (`Camera_IoT/Camera_IoT/src/`) khi bạn sẵn sàng — báo tôi lúc đó.
