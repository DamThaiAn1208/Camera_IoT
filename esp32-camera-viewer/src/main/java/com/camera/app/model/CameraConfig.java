package com.camera.app.model;

import java.net.URI;

/**
 * Central place for camera connection settings so the stream URL
 * is never duplicated across classes. Swap streamUrl to point at a
 * Raspberry Pi gateway later without touching any other file.
 */
public class CameraConfig {

    private String streamUrl = "http://192.168.1.66:81/stream";

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 5000;

    /**
     * Control requests (framesize/quality/fps) can take much longer than a
     * stream frame read: changing framesize makes the ESP32 reconfigure the
     * sensor (I2C writes, PSRAM framebuffer realloc), which can take several
     * seconds, especially while a stream is still running concurrently.
     */
    private int controlReadTimeoutMs = 15000;

    public String getStreamUrl() {
        return streamUrl;
    }

    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getControlReadTimeoutMs() {
        return controlReadTimeoutMs;
    }

    public void setControlReadTimeoutMs(int controlReadTimeoutMs) {
        this.controlReadTimeoutMs = controlReadTimeoutMs;
    }

    /**
     * Base URL for the ESP32's runtime control endpoint, derived from the
     * stream URL's host. The stock esp32-camera webserver serves
     * {@code /control} on port 80 while the MJPEG stream itself is on 81,
     * so the port is deliberately dropped here.
     */
    public String getControlBaseUrl() {
        URI streamUri = URI.create(streamUrl);
        return "http://" + streamUri.getHost() + "/control";
    }
}
