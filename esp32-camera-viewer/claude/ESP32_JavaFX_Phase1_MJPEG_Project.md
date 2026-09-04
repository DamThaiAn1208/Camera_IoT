# ESP32-S3 Camera Streaming - JavaFX Phase 1

## Objective

Build a Java application that directly receives and displays the ESP32-S3 Camera HTTP MJPEG stream.

Current architecture:

```
ESP32-S3 CAM
IP: 192.168.1.66
        |
        | HTTP MJPEG Stream
        |
        v
JavaFX Application
```

Phase 1 does NOT use Raspberry Pi yet.

---

## Requirements

Create a JavaFX desktop application with these features:

### 1. Camera Stream Display

- Connect directly to ESP32-S3 camera stream.
- Stream URL:

```
http://192.168.1.66:81/stream
```

- Display video in JavaFX ImageView.
- Support MJPEG (`multipart/x-mixed-replace`) stream format.

---

## Technology Stack

Use:

- Java 17
- JavaFX 21
- Maven project

Dependencies:

- javafx-controls
- javafx-fxml (optional)

---

## Application Structure

Create clean structure:

```
src/main/java

com.camera.app

├── Main.java
├── controller
│   └── CameraController.java
├── stream
│   └── MjpegStreamReader.java
├── model
│   └── CameraConfig.java
└── util
    └── FPSCounter.java
```

---

# Functional Requirements

## Main UI

Create JavaFX interface:

```
+--------------------------------+
| ESP32 Camera Viewer             |
+--------------------------------+

+--------------------------------+
|                                |
|                                |
|          Camera Stream         |
|          ImageView             |
|                                |
+--------------------------------+

FPS: XX
Status: Connected

[Start]
[Stop]
```

---

# MjpegStreamReader

Responsibilities:

- Open HTTP connection to ESP32.
- Read multipart MJPEG stream.
- Extract JPEG frames.
- Convert JPEG bytes into JavaFX Image.
- Update ImageView.

Example:

```
HTTP Response

--frame
Content-Type:image/jpeg

JPEG DATA

--frame
Content-Type:image/jpeg

JPEG DATA
```

---

# CameraController

Responsibilities:

- Handle Start button.
- Handle Stop button.
- Start/stop stream thread.
- Update UI status.

---

# FPS Counter

Calculate:

```
FPS = received frames / second
```

Display realtime FPS.

---

# Error Handling

Handle:

- ESP32 disconnected.
- Invalid URL.
- Network timeout.
- Stream stopped.

Show:

```
Status: Disconnected
```

---

# Configuration

Do NOT hardcode URL inside multiple files.

Create:

CameraConfig.java

Example:

```java
public class CameraConfig {

    private String streamUrl =
        "http://192.168.1.66:81/stream";

}
```

Later this URL can be replaced by Raspberry Pi gateway:

```
http://raspberry-pi-ip:8888/camera1
```

---

# Implementation Notes for Claude Code

Please generate:

1. Complete Maven project.
2. pom.xml.
3. JavaFX UI.
4. MJPEG parser.
5. Thread-safe frame updating.
6. Start/Stop stream functionality.
7. FPS display.
8. Clear comments explaining the streaming pipeline.

The final application must run with:

```
mvn clean javafx:run
```

or equivalent JavaFX launch command.

