package com.camera.app;

import com.camera.app.controller.CameraController;
import com.camera.app.model.CameraConfig;
import com.camera.app.model.Resolution;
import com.camera.app.stream.GatewayDiscoveryClient;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

/**
 * ESP32-S3 Camera Viewer - Phase 1.
 * Connects directly to the camera's HTTP MJPEG stream (no Raspberry
 * Pi gateway yet) and displays it live in a JavaFX ImageView.
 */
public class Main extends Application {

    private static final String PREF_RESOLUTION = "resolution.val";
    private static final String PREF_TARGET_FPS = "target.fps";
    private static final String PREF_QUALITY = "quality";

    private static final int DEFAULT_TARGET_FPS = 15;
    private static final int DEFAULT_QUALITY = 12;

    @Override
    public void start(Stage stage) {
        Preferences prefs = Preferences.userNodeForPackage(Main.class);

        CameraConfig config = new CameraConfig();
        // Ask the Raspberry Pi gateway which camera is currently registered
        // instead of relying on CameraConfig's hardcoded default IP. Falls
        // back to that default silently if the gateway isn't reachable yet.
        String discoveredStreamUrl = new GatewayDiscoveryClient().findFirstStreamUrl();
        if (discoveredStreamUrl != null) {
            config.setStreamUrl(discoveredStreamUrl);
        }
        System.out.println("Camera stream URL in use: " + config.getStreamUrl());

        ImageView imageView = new ImageView();
        imageView.setFitWidth(640);
        imageView.setFitHeight(480);
        imageView.setPreserveRatio(true);

        Label fpsLabel = new Label("FPS (live): 0");
        Label statusLabel = new Label("Status: Disconnected");

        Button startButton = new Button("Start");
        Button stopButton = new Button("Stop");
        stopButton.setDisable(true);

        CameraController controller = new CameraController(config, imageView, statusLabel, fpsLabel);

        startButton.setOnAction(e -> {
            controller.start();
            startButton.setDisable(true);
            stopButton.setDisable(false);
        });

        stopButton.setOnAction(e -> {
            controller.stop();
            startButton.setDisable(false);
            stopButton.setDisable(true);
        });

        HBox buttonBar = new HBox(10, startButton, stopButton);
        buttonBar.setAlignment(Pos.CENTER);

        // --- Camera Settings panel: resolution / target FPS / quality, applied on demand ---
        ComboBox<Resolution> resolutionCombo = new ComboBox<>(FXCollections.observableArrayList(Resolution.values()));
        resolutionCombo.setValue(Resolution.fromVal(prefs.getInt(PREF_RESOLUTION, Resolution.VGA.getVal())));

        Spinner<Integer> targetFpsSpinner = new Spinner<>(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30,
                        prefs.getInt(PREF_TARGET_FPS, DEFAULT_TARGET_FPS)));
        targetFpsSpinner.setEditable(false);

        Spinner<Integer> qualitySpinner = new Spinner<>(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 63,
                        prefs.getInt(PREF_QUALITY, DEFAULT_QUALITY)));
        qualitySpinner.setEditable(false);

        Button applyButton = new Button("Apply");
        applyButton.setDisable(true);
        applyButton.setOnAction(e -> {
            Resolution resolution = resolutionCombo.getValue();
            int targetFps = targetFpsSpinner.getValue();
            int quality = qualitySpinner.getValue();

            prefs.putInt(PREF_RESOLUTION, resolution.getVal());
            prefs.putInt(PREF_TARGET_FPS, targetFps);
            prefs.putInt(PREF_QUALITY, quality);

            controller.applySettings(resolution, targetFps, quality);
        });

        controller.setConnectionStateListener(connected -> applyButton.setDisable(!connected));

        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(10);
        settingsGrid.setVgap(8);
        settingsGrid.addRow(0, new Label("Resolution:"), resolutionCombo);
        settingsGrid.addRow(1, new Label("Target FPS:"), targetFpsSpinner);
        settingsGrid.addRow(2, new Label("Quality:"), qualitySpinner);

        VBox settingsBox = new VBox(10, settingsGrid, applyButton);
        settingsBox.setAlignment(Pos.CENTER_LEFT);
        settingsBox.setPadding(new Insets(10));
        TitledPane settingsPane = new TitledPane("Camera Settings", settingsBox);
        settingsPane.setCollapsible(false);

        VBox root = new VBox(10,
                new Label("ESP32 Camera Viewer"),
                imageView,
                fpsLabel,
                statusLabel,
                buttonBar,
                settingsPane);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(15));

        stage.setTitle("ESP32 Camera Viewer");
        stage.setScene(new Scene(root, 700, 760));
        stage.setOnCloseRequest(e -> controller.stop());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
