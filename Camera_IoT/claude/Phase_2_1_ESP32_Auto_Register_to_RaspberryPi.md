# Phase 2.1 - ESP32-S3 Camera Auto Registration to Raspberry Pi Spring Boot Gateway

## Objective

Modify ESP32-S3 Camera firmware so that after booting and connecting to
WiFi, it automatically registers itself with the Raspberry Pi Spring
Boot Gateway.

Architecture:

    ESP32-S3 CAM
          |
          | HTTP POST Register
          |
          v
    Raspberry Pi
    Spring Boot Gateway
          |
          | REST API
          |
          v
    JavaFX Application

## Raspberry Pi Gateway

Gateway IP:

    192.168.1.83

Registration endpoint:

    POST http://192.168.1.83:8080/api/cameras/register

## ESP32 Registration Flow

After startup:

1.  Connect to WiFi.
2.  Initialize camera server.
3.  Get local IP address.
4.  Send camera information to Raspberry Pi.
5.  Continue streaming normally.

Flow:

    ESP32 Boot
        |
    Connect WiFi
        |
    Get IP Address
        |
    POST /api/cameras/register
        |
    Start MJPEG Stream

## JSON Data Format

ESP32 sends:

``` json
{
  "cameraId": "ESP32_CAM_001",
  "ip": "192.168.1.66",
  "streamUrl": "http://192.168.1.66:81/stream"
}
```

## ESP32 Implementation Requirements

Add:

``` cpp
#include <WiFi.h>
#include <HTTPClient.h>
```

Create:

    registerCamera()

Function requirements:

-   Check WiFi connection.
-   Create HTTP POST request.
-   Send JSON body.
-   Print HTTP response.
-   Handle connection failure.

## Integration

Call registration after WiFi connection:

    setup()

    1. Serial.begin()
    2. Connect WiFi
    3. registerCamera()
    4. Start camera streaming

## Testing

Start Raspberry Pi Gateway:

    mvn spring-boot:run

Power on ESP32.

Expected Serial output:

    WiFi connected
    Camera IP: 192.168.1.66
    Register response: 200

Check Raspberry Pi:

    GET http://localhost:8080/api/cameras

Expected:

``` json
[
 {
  "cameraId":"ESP32_CAM_001",
  "ip":"192.168.1.66",
  "streamUrl":"http://192.168.1.66:81/stream"
 }
]
```

## Future Improvements

After this phase:

1.  Replace temporary memory storage with SQLite.
2.  Add camera online/offline status.
3.  Add heartbeat mechanism.
4.  Support multiple ESP32 cameras.
5.  Update JavaFX application to discover cameras from Raspberry Pi.
