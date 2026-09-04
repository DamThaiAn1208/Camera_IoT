# Deploy RaspberryPi-Gateway (Spring Boot) lên Raspberry Pi

Java là bytecode chạy trên JVM — không cần build trên đúng kiến trúc ARM của Pi. Cách nhanh nhất:
**build file `.jar` ở máy Windows (đã test chạy được ở đây), rồi copy sang Pi, Pi chỉ cần cài JRE để chạy.**

---

## 0. Chuẩn bị: bật SSH trên Pi (nếu chưa)

```bash
sudo raspi-config
# Interface Options > SSH > Enable
```

Lấy IP hoặc dùng hostname mDNS đã có sẵn (xem [README.md](README.md)):
```
raspberry.local
```

---

## 1. Cài Java Runtime trên Pi

```bash
ssh camera-eu@raspberry.local
sudo apt update
sudo apt install -y openjdk-17-jre-headless
java -version
```

---

## 2. Build jar ở máy Windows

Trong thư mục `RaspberryPi-Gateway/` (máy Windows, dùng Git Bash hoặc PowerShell):

```bash
./mvnw clean package -DskipTests
```

Ra file: `target/raspberry-camera-gateway-1.0.0.jar`

---

## 3. Copy jar sang Pi

```bash
scp target/raspberry-camera-gateway-1.0.0.jar camera-eu@raspberry.local:/home/camera-eu/gateway.jar
```

(Windows không có `scp` sẵn thì dùng Git Bash — nó có kèm sẵn — hoặc WinSCP nếu thích giao diện.)

---

## 4. Chạy thử thủ công trên Pi

```bash
ssh camera-eu@raspberry.local
java -jar /home/camera-eu/gateway.jar
```

Mở trình duyệt máy khác cùng mạng: `http://raspberry.local:8080/api/cameras` → phải trả về `[]` (danh sách rỗng, do chưa có camera nào đăng ký) là chạy đúng.

Ctrl+C để dừng test thủ công trước khi qua bước 5.

---

## 5. Chạy tự động khi Pi khởi động (systemd)

Copy file `gateway.service` (cùng thư mục với tài liệu này) vào Pi:

```bash
scp gateway.service camera-eu@raspberry.local:/tmp/gateway.service
ssh camera-eu@raspberry.local
sudo mv /tmp/gateway.service /etc/systemd/system/gateway.service
sudo systemctl daemon-reload
sudo systemctl enable --now gateway
sudo systemctl status gateway
```

Xem log:
```bash
journalctl -u gateway -f
```

---

## 6. Quy trình cập nhật code sau này

Mỗi lần sửa code Java ở Windows:
```bash
./mvnw clean package -DskipTests
scp target/raspberry-camera-gateway-1.0.0.jar camera-eu@raspberry.local:/home/camera-eu/gateway.jar
ssh camera-eu@raspberry.local "sudo systemctl restart gateway"
```

---

## Cách khác (không dùng ở đây nhưng đáng biết)

- **Git clone trực tiếp trên Pi + build bằng `./mvnw` trên Pi**: tiện nếu bạn sửa code ngay trên Pi qua SSH/VS Code Remote-SSH, khỏi phải copy jar qua lại. Cần cài `openjdk-17-jdk` (bản đầy đủ, không phải `-jre`) trên Pi.
- **VS Code Remote-SSH**: mở thẳng thư mục trên Pi từ VS Code ở Windows, sửa/debug như local — phù hợp nếu bạn sẽ sửa gateway thường xuyên.
