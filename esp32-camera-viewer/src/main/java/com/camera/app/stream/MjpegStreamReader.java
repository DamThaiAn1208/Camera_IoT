package com.camera.app.stream;

import com.camera.app.model.CameraConfig;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Arrays;

/**
 * Reads an HTTP MJPEG (multipart/x-mixed-replace) stream and emits
 * one decoded JavaFX Image per JPEG frame found.
 *
 * Frames are located by scanning the raw byte stream for JPEG
 * start-of-image (0xFFD8) / end-of-image (0xFFD9) markers rather than
 * strictly parsing multipart boundaries/headers, since ESP32-CAM
 * firmware boundary formatting varies. This makes extraction robust
 * to minor deviations in the multipart framing.
 */
public class MjpegStreamReader implements Runnable {

    /** Callbacks fired from the background streaming thread; deliver to the UI via Platform.runLater. */
    public interface Listener {
        void onFrame(Image image);
        void onConnected();
        void onDisconnected(String reason);
    }

    private static final byte[] JPEG_SOI = {(byte) 0xFF, (byte) 0xD8};
    private static final byte[] JPEG_EOI = {(byte) 0xFF, (byte) 0xD9};

    private final CameraConfig config;
    private final Listener listener;

    private volatile boolean running = false;
    private HttpURLConnection connection;

    public MjpegStreamReader(CameraConfig config, Listener listener) {
        this.config = config;
        this.listener = listener;
    }

    /** Signals the read loop to stop and closes the underlying connection to unblock any in-flight read. */
    public void stop() {
        running = false;
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public void run() {
        running = true;
        try {
            connection = (HttpURLConnection) URI.create(config.getStreamUrl()).toURL().openConnection();
            connection.setConnectTimeout(config.getConnectTimeoutMs());
            connection.setReadTimeout(config.getReadTimeoutMs());
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                listener.onDisconnected("HTTP " + connection.getResponseCode());
                return;
            }

            listener.onConnected();
            readFrames(connection.getInputStream());
            listener.onDisconnected("Stream stopped");
        } catch (IOException e) {
            if (running) {
                listener.onDisconnected("Network error: " + e.getMessage());
            } else {
                listener.onDisconnected("Stream stopped");
            }
        } finally {
            running = false;
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void readFrames(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;

        while (running && (bytesRead = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
            byte[] data = buffer.toByteArray();

            int searchFrom = 0;
            int consumedTo = 0;
            while (true) {
                int soi = indexOf(data, JPEG_SOI, searchFrom);
                if (soi < 0) {
                    break;
                }
                int eoi = indexOf(data, JPEG_EOI, soi + JPEG_SOI.length);
                if (eoi < 0) {
                    break;
                }
                int end = eoi + JPEG_EOI.length;
                byte[] jpeg = Arrays.copyOfRange(data, soi, end);
                emitFrame(jpeg);
                consumedTo = end;
                searchFrom = end;
            }

            buffer.reset();
            if (consumedTo < data.length) {
                buffer.write(data, consumedTo, data.length - consumedTo);
            }
        }
    }

    private void emitFrame(byte[] jpegBytes) {
        Image image = new Image(new ByteArrayInputStream(jpegBytes));
        if (!image.isError()) {
            listener.onFrame(image);
        }
    }

    private static int indexOf(byte[] data, byte[] pattern, int fromIndex) {
        int limit = data.length - pattern.length;
        outer:
        for (int i = Math.max(fromIndex, 0); i <= limit; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
