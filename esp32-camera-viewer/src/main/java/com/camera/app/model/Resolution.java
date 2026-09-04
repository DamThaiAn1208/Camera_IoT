package com.camera.app.model;

/**
 * Camera resolutions supported by the ESP32 firmware's
 * {@code /control?var=framesize} endpoint. `val` mirrors the
 * `framesize_t` enum order of the esp32-camera library — verify
 * against the actual firmware's sensor.h if frame sizes look wrong,
 * since the order can shift between library versions.
 */
public enum Resolution {
    QQVGA(1, "QQVGA (160x120)"),
    QCIF(2, "QCIF (176x144)"),
    HQVGA(3, "HQVGA (240x176)"),
    QVGA(5, "QVGA (320x240)"),
    CIF(6, "CIF (400x296)"),
    HVGA(7, "HVGA (480x320)"),
    VGA(8, "VGA (640x480)"),
    SVGA(9, "SVGA (800x600)"),
    XGA(10, "XGA (1024x768)"),
    HD(11, "HD (1280x720)"),
    SXGA(12, "SXGA (1280x1024)"),
    UXGA(13, "UXGA (1600x1200)");

    private final int val;
    private final String label;

    Resolution(int val, String label) {
        this.val = val;
        this.label = label;
    }

    public int getVal() {
        return val;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Resolution fromVal(int val) {
        for (Resolution r : values()) {
            if (r.val == val) {
                return r;
            }
        }
        return VGA;
    }
}
