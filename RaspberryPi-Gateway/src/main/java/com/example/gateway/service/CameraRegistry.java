package com.example.gateway.service;

import com.example.gateway.model.CameraInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory camera directory. Registering an already-known cameraId
 * overwrites its previous entry (e.g. after the camera reboots with a
 * new IP). Replace with SQLite once persistence is needed.
 */
@Service
public class CameraRegistry {

    private final Map<String, CameraInfo> cameras = new ConcurrentHashMap<>();

    public void register(CameraInfo camera) {
        cameras.put(camera.getCameraId(), camera);
    }

    public List<CameraInfo> getAll() {
        Collection<CameraInfo> values = cameras.values();
        return List.copyOf(values);
    }
}
