package com.camera.app.controller;

import com.camera.app.model.CameraConfig;
import com.camera.app.model.Resolution;
import com.camera.app.stream.CameraControlClient;
import com.camera.app.stream.MjpegStreamReader;
import com.camera.app.util.FPSCounter;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Wires the Start/Stop buttons to the streaming thread and pushes
 * decoded frames + status/FPS updates back onto the JavaFX
 * application thread. Also applies runtime camera settings
 * (resolution/target fps/quality) via the ESP32 control endpoint.
 */
public class CameraController {

    private final CameraConfig config;
    private final ImageView imageView;
    private final Label statusLabel;
    private final Label fpsLabel;
    private final FPSCounter fpsCounter = new FPSCounter();
    private final CameraControlClient controlClient;

    private Thread streamThread;
    private MjpegStreamReader streamReader;
    private volatile boolean streaming = false;
    private Consumer<Boolean> connectionStateListener;

    public CameraController(CameraConfig config, ImageView imageView, Label statusLabel, Label fpsLabel) {
        this.config = config;
        this.imageView = imageView;
        this.statusLabel = statusLabel;
        this.fpsLabel = fpsLabel;
        this.controlClient = new CameraControlClient(config);
    }

    /** Notified (on the FX thread) whenever the stream connects or disconnects, so the UI can enable/disable Apply. */
    public void setConnectionStateListener(Consumer<Boolean> listener) {
        this.connectionStateListener = listener;
    }

    public boolean isConnected() {
        return streaming;
    }

    public void start() {
        if (streaming) {
            return;
        }
        streaming = true;
        fpsCounter.reset();
        setStatus("Connecting...");

        streamReader = new MjpegStreamReader(config, new MjpegStreamReader.Listener() {
            @Override
            public void onFrame(Image image) {
                fpsCounter.frameReceived();
                Platform.runLater(() -> {
                    imageView.setImage(image);
                    fpsLabel.setText("FPS (live): " + fpsCounter.getFps());
                });
            }

            @Override
            public void onConnected() {
                Platform.runLater(() -> {
                    setStatus("Connected");
                    notifyConnectionState(true);
                });
            }

            @Override
            public void onDisconnected(String reason) {
                streaming = false;
                Platform.runLater(() -> {
                    setStatus("Disconnected (" + reason + ")");
                    notifyConnectionState(false);
                });
            }
        });

        streamThread = new Thread(streamReader, "mjpeg-stream-reader");
        streamThread.setDaemon(true);
        streamThread.start();
    }

    public void stop() {
        if (!streaming) {
            return;
        }
        streaming = false;
        if (streamReader != null) {
            streamReader.stop();
        }
        setStatus("Disconnected");
        fpsLabel.setText("FPS (live): 0");
        notifyConnectionState(false);
    }

    /**
     * Applies resolution/target-fps/quality to the camera via the ESP32
     * control endpoint. Runs off the FX thread and never throws; the
     * outcome is reported through the status label.
     */
    public void applySettings(Resolution resolution, int targetFps, int quality) {
        if (!streaming) {
            setStatus("Not connected - cannot apply settings");
            return;
        }

        setStatus("Applying " + resolution + " @ " + targetFps + "fps...");

        CompletableFuture
                .supplyAsync(() -> null)
                .thenCompose(v -> controlClient.setParam("framesize", resolution.getVal()))
                .thenCompose(v -> controlClient.setParam("quality", quality))
                .thenCompose(v -> controlClient.setParam("fps", targetFps))
                .whenComplete((v, error) -> Platform.runLater(() -> {
                    if (error == null) {
                        setStatus("Applied " + resolution + " @ " + targetFps + "fps");
                    } else {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        setStatus("Failed to apply settings (" + cause.getMessage() + ")");
                        cause.printStackTrace();
                    }
                }));
    }

    private void notifyConnectionState(boolean connected) {
        if (connectionStateListener != null) {
            connectionStateListener.accept(connected);
        }
    }

    private void setStatus(String text) {
        statusLabel.setText("Status: " + text);
    }
}
