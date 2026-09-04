#include <Arduino.h>
#include <WiFi.h>

#include "camera_registration.h"
#include "camera_setup.h"
#include "control_server.h"
#include "stream_server.h"
#include "wifi_credentials.h"

void setup() {
    Serial.begin(115200);
    // Board has no separate UART-USB bridge chip; give the native USB-CDC
    // link time to enumerate so early log lines aren't dropped.
    delay(2000);
    Serial.println("Boot: starting camera init...");

    if (!initCamera()) {
        Serial.println("Camera init failed");
        return;
    }
    Serial.println("Camera ready");

    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    Serial.print("Connecting to WiFi");
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }
    Serial.println();
    Serial.printf("WiFi connected, IP: %s\n", WiFi.localIP().toString().c_str());
    // Modem sleep (default on) delays incoming packets to save power,
    // which shows up as stutter/lag on the MJPEG stream.
    WiFi.setSleep(false);

    registerCamera();
    startCameraServer();
    startControlServer();
}

void loop() {
    static unsigned long lastHeartbeat = 0;
    if (millis() - lastHeartbeat > 3000) {
        lastHeartbeat = millis();
        if (WiFi.status() == WL_CONNECTED) {
            Serial.printf("tick - WiFi OK, stream at http://%s:%u/stream\n",
                          WiFi.localIP().toString().c_str(), CAMERA_STREAM_PORT);
        } else {
            Serial.printf("tick - WiFi not connected (status=%d)\n", WiFi.status());
        }
    }
    handleCameraServer();
    handleControlServer();
}
