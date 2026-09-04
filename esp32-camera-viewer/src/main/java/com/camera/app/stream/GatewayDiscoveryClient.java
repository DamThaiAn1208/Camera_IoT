package com.camera.app.stream;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Looks up registered cameras from the Raspberry Pi gateway's REST API
 * ({@code GET /api/cameras}) so the app doesn't need a hardcoded ESP32
 * IP. The gateway itself is addressed by its mDNS hostname
 * (raspberry.local) rather than a raw IP, matching how the ESP32
 * firmware finds the same Pi (see
 * Camera_IoT/Camera_IoT/src/camera_registration.cpp).
 */
public class GatewayDiscoveryClient {

    private static final String GATEWAY_CAMERAS_URL = "http://raspberry.local:8080/api/cameras";
    private static final int TIMEOUT_MS = 3000;

    /**
     * Returns the first registered camera's stream URL, or {@code null}
     * if the gateway is unreachable or no camera has registered yet.
     */
    public String findFirstStreamUrl() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(GATEWAY_CAMERAS_URL).toURL().openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("GatewayDiscoveryClient: gateway returned HTTP " + responseCode);
                return null;
            }

            JSONArray cameras = new JSONArray(readBody(connection.getInputStream()));
            if (cameras.isEmpty()) {
                System.out.println("GatewayDiscoveryClient: no cameras registered yet");
                return null;
            }

            JSONObject first = cameras.getJSONObject(0);
            String streamUrl = first.optString("streamUrl", null);
            System.out.println("GatewayDiscoveryClient: discovered streamUrl = " + streamUrl);
            return streamUrl;
        } catch (IOException e) {
            System.out.println("GatewayDiscoveryClient: gateway unreachable (" + e.getMessage() + ")");
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readBody(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
