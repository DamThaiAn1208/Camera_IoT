package com.example.gateway.controller;

import com.example.gateway.model.CameraInfo;
import com.example.gateway.service.CameraRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cameras")
public class CameraController {

    private final CameraRegistry registry;

    public CameraController(CameraRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/register")
    public CameraInfo register(@RequestBody CameraInfo camera) {
        registry.register(camera);
        return camera;
    }

    @GetMapping
    public List<CameraInfo> list() {
        return registry.getAll();
    }
}
