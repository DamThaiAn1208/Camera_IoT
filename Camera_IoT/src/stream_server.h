#pragma once

#include <cstdint>

constexpr uint16_t CAMERA_STREAM_PORT = 81;

// Starts listening for MJPEG stream clients on CAMERA_STREAM_PORT.
void startCameraServer();

// Non-blocking: does at most one unit of work per call (accept a new
// client, or send a single frame to the current one) and returns
// immediately otherwise. Must be called every loop() iteration so it
// never starves handleControlServer() while a client is streaming.
void handleCameraServer();

// Runtime upper bound on frames sent per second (actual rate can be
// lower if the sensor/WiFi can't keep up). Clamped to >= 1.
void setTargetFps(int fps);
int getTargetFps();
