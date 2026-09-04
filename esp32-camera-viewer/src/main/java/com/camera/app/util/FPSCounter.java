package com.camera.app.util;

/**
 * Counts frames received over rolling one-second windows and
 * reports the most recently completed window's rate.
 */
public class FPSCounter {

    private long windowStartMs = System.currentTimeMillis();
    private int framesInWindow = 0;
    private volatile int lastFps = 0;

    public synchronized void frameReceived() {
        long now = System.currentTimeMillis();
        framesInWindow++;

        long elapsed = now - windowStartMs;
        if (elapsed >= 1000) {
            lastFps = (int) Math.round(framesInWindow * 1000.0 / elapsed);
            framesInWindow = 0;
            windowStartMs = now;
        }
    }

    public synchronized void reset() {
        windowStartMs = System.currentTimeMillis();
        framesInWindow = 0;
        lastFps = 0;
    }

    public int getFps() {
        return lastFps;
    }
}
