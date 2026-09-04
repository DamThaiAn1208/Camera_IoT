package com.example.gateway.service;

import com.example.gateway.model.CameraInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Subscribes to the retained camera status messages published by the
 * ESP32 firmware (see Camera_IoT/Camera_IoT/src/camera_registration.cpp)
 * and keeps CameraRegistry in sync. Replaces the old HTTP push-based
 * /api/cameras/register flow, which stays around only as a manual/test
 * entry point.
 *
 * Runs on the same Pi as Mosquitto, so the broker is always localhost —
 * no mDNS lookup needed here (unlike the ESP32, which is a separate
 * device on the network).
 */
@Component
public class MqttCameraSubscriber {

    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final String STATUS_TOPIC_FILTER = "camera/+/status";

    private final CameraRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MqttCameraSubscriber(CameraRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void start() {
        try {
            MqttClient client = new MqttClient(BROKER_URL, "gateway-" + UUID.randomUUID(), null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            client.connect(options);
            client.subscribe(STATUS_TOPIC_FILTER, (topic, message) -> {
                try {
                    CameraInfo info = objectMapper.readValue(message.getPayload(), CameraInfo.class);
                    registry.register(info);
                    System.out.println("MqttCameraSubscriber: registered " + info.getCameraId()
                            + " @ " + info.getIp());
                } catch (Exception e) {
                    System.err.println("MqttCameraSubscriber: failed to parse message on "
                            + topic + ": " + e.getMessage());
                }
            });

            System.out.println("MqttCameraSubscriber: subscribed to " + STATUS_TOPIC_FILTER
                    + " on " + BROKER_URL);
        } catch (MqttException e) {
            System.err.println("MqttCameraSubscriber: failed to connect to broker: " + e.getMessage());
        }
    }
}
