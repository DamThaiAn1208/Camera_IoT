#pragma once

// Registers this camera with the Raspberry Pi gateway so the JavaFX app
// can discover it. Requires WiFi to already be connected.
//
// The gateway's address is resolved via mDNS (looks up "raspberry.local")
// instead of a hardcoded IP, then a retained status message (camera id,
// IP, stream URL) is published to the Mosquitto broker running on the
// same Pi. Prints progress/failures to Serial.
void registerCamera();
