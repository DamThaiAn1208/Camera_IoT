package com.camera.app.stream;

import com.camera.app.model.CameraConfig;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Sends runtime camera control requests to the ESP32 firmware's
 * {@code /control?var=<name>&val=<value>} endpoint (framesize,
 * quality, fps). Requests run on the common ForkJoinPool via
 * CompletableFuture so callers never block the UI thread.
 */
public class CameraControlClient {

    private final CameraConfig config;

    public CameraControlClient(CameraConfig config) {
        this.config = config;
    }

    public CompletableFuture<Void> setParam(String var, int val) {
        return CompletableFuture.runAsync(() -> sendControlRequest(var, val));
    }

    private void sendControlRequest(String var, int val) {
        String url = config.getControlBaseUrl() + "?var=" + var + "&val=" + val;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(config.getConnectTimeoutMs());
            connection.setReadTimeout(config.getControlReadTimeoutMs());
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new ControlRequestException(var + "=" + val + " -> HTTP " + responseCode);
            }
        } catch (IOException e) {
            throw new ControlRequestException(var + "=" + val + ": " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static class ControlRequestException extends RuntimeException {
        public ControlRequestException(String message) {
            super(message);
        }

        public ControlRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
