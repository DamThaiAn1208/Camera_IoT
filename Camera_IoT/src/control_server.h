#pragma once

#include <cstdint>

constexpr uint16_t CONTROL_SERVER_PORT = 80;

// Starts listening for camera control requests on CONTROL_SERVER_PORT.
// Supported: GET /control?var=framesize&val=<framesize_t>
//            GET /control?var=quality&val=<0-63>
//            GET /control?var=fps&val=<n>
void startControlServer();

// Accepts one pending client (if any) and answers its request. Call
// this from loop().
void handleControlServer();
