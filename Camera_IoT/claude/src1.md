# ESP32-S3 Camera Streaming System - Development Requirement

## Project Goal

Build a simple camera streaming system with:

- 1 ESP32-S3 CAM
- 1 Raspberry Pi
- 1 Java Application

All devices are in the same local network.

The current goal is only to make a working prototype with **one camera**. Do not consider multiple cameras, cloud, NAT, or remote network access yet.

---

# System Architecture

```
+-------------+
| ESP32-S3 CAM|
| OV2640      |
+-------------+
       |
       | HTTP MJPEG Stream
       |
       v
+-------------+
| Java App    |
| Viewer      |
+-------------+


+-------------+
| Raspberry Pi|
| Device Info |
| API Server  |
+-------------+
       ^
       |
       | REST API
       |
+-------------+
| Java App    |
+-------------+
```

---

# Main Workflow

## 1. ESP32-S3 Camera

ESP32 should:

- Connect to WiFi
- Start HTTP server
- Capture images from OV2640 camera
- Compress images to JPEG
- Provide MJPEG streaming endpoint

Example:

```
http://ESP32_IP:81/stream
```

The stream should continuously return camera frames.

---

## 2. Raspberry Pi

For this prototype, Raspberry Pi only works as a camera information server.

Responsibilities:

- Store camera information
- Provide REST API for Java App
- Return camera IP and stream URL

Example stored data:

```json
{
  "cameraId": "CAM001",
  "name": "ESP32 Camera",
  "ip": "192.168.1.50",
  "streamUrl": "http://192.168.1.50:81/stream",
  "status": "ONLINE"
}
```

Database can use:

- SQLite

---

# Raspberry Pi API Design

## Get Camera Information

Endpoint:

```
GET /api/cameras
```

Response:

```json
[
  {
    "cameraId": "CAM001",
    "name": "ESP32 Camera",
    "streamUrl": "http://192.168.1.50:81/stream",
    "status": "ONLINE"
  }
]
```

---

# Java Application

Java application should:

## 1. Connect to Raspberry Pi API

Flow:

```
Java App
    |
    | HTTP GET
    |
Raspberry Pi
    |
    | Return camera information
    |
Java App
```

---

## 2. Display Camera Stream

After receiving:

```
http://192.168.1.50:81/stream
```

Java connects directly to ESP32.

Flow:

```
JavaFX ImageView

        |
        |
        v

ESP32 MJPEG Stream
```

Requirements:

- Display real-time video
- Handle frame updates
- Show connection status

---

# Recommended Technology

## ESP32

Language:

- Arduino C++

Libraries:

- WiFi.h
- esp_camera.h
- WebServer.h

---

## Raspberry Pi

Backend:

Option 1:

- Python Flask

Option 2:

- Java Spring Boot

Database:

- SQLite

---

## Java Application

Framework:

- JavaFX

Functions:

- Get camera list
- Connect stream URL
- Display video

---

# Development Order

## Phase 1: ESP32 Camera

Implement:

- WiFi connection
- Camera initialization
- HTTP MJPEG stream

Test:

Open browser:

```
http://ESP32_IP:81/stream
```

Expected result:

Live camera stream appears.

---

## Phase 2: Raspberry Pi API

Implement:

- Store ESP32 camera information
- Create REST API

Test:

Open:

```
http://RASPBERRY_PI_IP/api/cameras
```

Expected:

Return camera JSON.

---

## Phase 3: Java Application

Implement:

1. Call Raspberry Pi API
2. Receive stream URL
3. Open ESP32 stream
4. Display video

---

# Important Notes

Current scope:

✅ One ESP32 camera
✅ Same WiFi network
✅ Direct ESP32 streaming
✅ Raspberry Pi only manages camera information

Not included yet:

❌ Multiple cameras
❌ Cloud server
❌ Remote access
❌ Video recording
❌ RTSP/H264
❌ Load balancing

Future architecture can extend:

```
Multiple ESP32 CAM
        |
        |
Raspberry Pi Gateway
        |
        |
Java Application
```
