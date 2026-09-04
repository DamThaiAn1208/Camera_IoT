#include "stream_server.h"

#include <Arduino.h>
#include <WiFi.h>
#include <esp_camera.h>

namespace {

constexpr const char* STREAM_BOUNDARY = "frameboundary";
constexpr const char* STREAM_CONTENT_TYPE =
    "multipart/x-mixed-replace;boundary=frameboundary";

WiFiServer streamServer(CAMERA_STREAM_PORT);
volatile int targetFps = 15;

// Persists across loop() calls so handleCameraServer() can serve one
// client incrementally (one frame per call) instead of blocking inside
// a single call until the client disconnects.
WiFiClient streamClient;
bool streamClientActive = false;
unsigned long lastFrameTime = 0;

// Drains the client's request line/headers — this prototype serves the
// same MJPEG stream regardless of the requested path.
void consumeRequest(WiFiClient& client) {
    unsigned long start = millis();
    while (client.connected() && client.available() == 0 && millis() - start < 1000) {
        delay(1);
    }
    while (client.available()) {
        client.readStringUntil('\n');
    }
}

// Sends exactly one JPEG frame as a multipart part. Stops the client on
// capture failure so the caller drops back to accepting a new one.
void sendFrame(WiFiClient& client) {
    camera_fb_t* fb = esp_camera_fb_get();
    if (!fb) {
        Serial.println("Camera capture failed");
        client.stop();
        return;
    }

    client.printf("--%s\r\n", STREAM_BOUNDARY);
    client.printf("Content-Type: image/jpeg\r\nContent-Length: %u\r\n\r\n", fb->len);
    client.write(fb->buf, fb->len);
    client.print("\r\n");

    esp_camera_fb_return(fb);
}

}  // namespace

void startCameraServer() {
    streamServer.begin();
    Serial.printf("MJPEG stream ready at http://%s:%u/stream\n",
                   WiFi.localIP().toString().c_str(), CAMERA_STREAM_PORT);
} 

void handleCameraServer() {
    if (streamClientActive) {
        if (!streamClient.connected()) {
            streamClient.stop();
            streamClientActive = false;
            return;
        }

        unsigned long now = millis();
        unsigned long frameInterval = 1000 / targetFps;
        if (now - lastFrameTime < frameInterval) {
            return;
        }
        lastFrameTime = now;

        sendFrame(streamClient);
        if (!streamClient.connected()) {
            streamClient.stop();
            streamClientActive = false;
        }
        return;
    }

    WiFiClient newClient = streamServer.available();
    if (!newClient) {
        return;
    }

    consumeRequest(newClient);
    newClient.print("HTTP/1.1 200 OK\r\n");
    newClient.printf("Content-Type: %s\r\n\r\n", STREAM_CONTENT_TYPE);

    streamClient = newClient;
    streamClientActive = true;
    lastFrameTime = 0;
}

void setTargetFps(int fps) {
    targetFps = (fps > 0) ? fps : 1;
}

int getTargetFps() {
    return targetFps;
}
