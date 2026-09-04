#include "control_server.h"

#include <Arduino.h>
#include <WiFi.h>
#include <esp_camera.h>

#include "stream_server.h"

namespace {

WiFiServer controlServer(CONTROL_SERVER_PORT);

// Returns the value for `key` in a "var=fps&val=10"-style query string,
// or "" if not present.
String queryValue(const String& query, const String& key) {
    int start = 0;
    while (start < (int)query.length()) {
        int amp = query.indexOf('&', start);
        String pair = (amp == -1) ? query.substring(start) : query.substring(start, amp);

        int eq = pair.indexOf('=');
        if (eq != -1 && pair.substring(0, eq) == key) {
            return pair.substring(eq + 1);
        }
        if (amp == -1) {
            break;
        }
        start = amp + 1;
    }
    return "";
}

void sendResponse(WiFiClient& client, bool ok) {
    if (ok) {
        client.print("HTTP/1.1 200 OK\r\nAccess-Control-Allow-Origin: *\r\nContent-Length: 0\r\n\r\n");
    } else {
        client.print("HTTP/1.1 500 Internal Server Error\r\nContent-Length: 0\r\n\r\n");
    }
}

void handleClient(WiFiClient& client) {
    unsigned long start = millis();
    while (client.connected() && client.available() == 0 && millis() - start < 1000) {
        delay(1);
    }
    if (!client.available()) {
        return;
    }

    String requestLine = client.readStringUntil('\n');
    while (client.available()) {
        client.readStringUntil('\n');  // drain remaining headers
    }

    // requestLine looks like: "GET /control?var=fps&val=10 HTTP/1.1"
    int pathStart = requestLine.indexOf(' ') + 1;
    int pathEnd = requestLine.indexOf(' ', pathStart);
    if (pathStart <= 0 || pathEnd < 0) {
        sendResponse(client, false);
        return;
    }
    String path = requestLine.substring(pathStart, pathEnd);

    int qPos = path.indexOf('?');
    if (qPos == -1) {
        client.print("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n");
        return;
    }
    String query = path.substring(qPos + 1);

    String variable = queryValue(query, "var");
    int val = queryValue(query, "val").toInt();

    sensor_t* sensor = esp_camera_sensor_get();
    bool ok = (sensor != nullptr);

    if (!ok) {
        // no-op, ok already false
    } else if (variable == "framesize") {
        ok = sensor->set_framesize(sensor, (framesize_t)val) == 0;
    } else if (variable == "quality") {
        ok = sensor->set_quality(sensor, val) == 0;
    } else if (variable == "fps") {
        setTargetFps(val);
    } else {
        ok = false;
    }

    sendResponse(client, ok);
}

}  // namespace

void startControlServer() {
    controlServer.begin();
    Serial.printf("Control API ready at http://%s:%u/control\n",
                   WiFi.localIP().toString().c_str(), CONTROL_SERVER_PORT);
}

void handleControlServer() {
    WiFiClient client = controlServer.available();
    if (!client) {
        return;
    }

    handleClient(client);
    client.stop();
}
