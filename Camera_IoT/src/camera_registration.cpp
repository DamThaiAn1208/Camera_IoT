#include "camera_registration.h"

#include <Arduino.h>
#include <WiFi.h>
#include <ESPmDNS.h>
#include <PubSubClient.h>

#include "stream_server.h"

namespace {

// Hostname of the Raspberry Pi gateway, without ".local" — matches the
// avahi-daemon hostname configured on the Pi (see
// RaspberryPi-Gateway/raspberrypi-setup/README.md).
constexpr const char* GATEWAY_MDNS_HOST = "raspberry";
constexpr uint16_t MQTT_BROKER_PORT = 1883;
constexpr const char* CAMERA_ID = "ESP32_CAM_001";
constexpr int MDNS_QUERY_ATTEMPTS = 5;
constexpr unsigned long MDNS_RETRY_DELAY_MS = 1000;

WiFiClient mqttNetClient;
PubSubClient mqttClient(mqttNetClient);

// mDNS queries can miss the first try right after WiFi just connected, so
// give it a few attempts before giving up.
bool resolveGatewayIp(IPAddress& outIp) {
    if (!MDNS.begin("esp32cam")) {
        Serial.println("registerCamera: mDNS init failed");
        return false;
    }

    for (int attempt = 1; attempt <= MDNS_QUERY_ATTEMPTS; attempt++) {
        IPAddress ip = MDNS.queryHost(GATEWAY_MDNS_HOST);
        if (ip != IPAddress(0, 0, 0, 0)) {
            outIp = ip;
            return true;
        }
        Serial.printf("registerCamera: mDNS query for %s.local missed (attempt %d/%d)\n",
                       GATEWAY_MDNS_HOST, attempt, MDNS_QUERY_ATTEMPTS);
        delay(MDNS_RETRY_DELAY_MS);
    }
    return false;
}

}  // namespace

void registerCamera() {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("registerCamera: WiFi not connected, skipping");
        return;
    }

    IPAddress gatewayIp;
    if (!resolveGatewayIp(gatewayIp)) {
        Serial.printf("registerCamera: could not resolve %s.local via mDNS, skipping\n",
                       GATEWAY_MDNS_HOST);
        return;
    }
    Serial.printf("registerCamera: gateway resolved to %s\n", gatewayIp.toString().c_str());

    mqttClient.setServer(gatewayIp, MQTT_BROKER_PORT);
    if (!mqttClient.connect(CAMERA_ID)) {
        Serial.printf("registerCamera: MQTT connect failed, rc=%d\n", mqttClient.state());
        return;
    }

    String ip = WiFi.localIP().toString();
    String streamUrl = "http://" + ip + ":" + String(CAMERA_STREAM_PORT) + "/stream";
    String payload = "{\"cameraId\":\"" + String(CAMERA_ID) +
                      "\",\"ip\":\"" + ip +
                      "\",\"streamUrl\":\"" + streamUrl + "\"}";

    char topic[64];
    snprintf(topic, sizeof(topic), "camera/%s/status", CAMERA_ID);

    // Retained so the gateway sees the camera's last known status even if
    // it subscribes after this message was published.
    bool ok = mqttClient.publish(topic, payload.c_str(), true);
    Serial.printf("registerCamera: publish to %s -> %s\n", topic, ok ? "ok" : "failed");

    mqttClient.disconnect();
}
