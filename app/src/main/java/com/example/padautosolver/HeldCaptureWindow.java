package com.example.padautosolver;

/** A cached image from before the press or after release is not held-tooltip evidence. */
final class HeldCaptureWindow {
    static boolean contains(long started, long frameTime, long now, long holdMs) {
        return holdMs >= 2000 && frameTime >= started + 700 && frameTime <= now
                && now < started + holdMs - 200;
    }
}
