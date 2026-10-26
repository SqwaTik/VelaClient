package dev.sqwat.vela.ui.animation;

public final class Motion {
    private Motion() {}

    /** Shared response for scrolling lists; callers retain ownership of bounds and direct thumb dragging. */
    public static float scroll(float current, float target, float deltaSeconds) {
        float value = approach(current, target, 18, deltaSeconds);
        return Math.abs(value-target)<.001f ? target : value;
    }

    public static float approach(float current, float target, float speed, float deltaSeconds) {
        if (deltaSeconds <= 0) return current;
        float t = 1.0f - (float) Math.exp(-speed * Math.min(deltaSeconds, 0.1f));
        return current + (target - current) * t;
    }

    public static float easeOutCubic(float value) {
        float t = clamp01(value);
        return 1.0f - (float) Math.pow(1.0f - t, 3);
    }

    public static float clamp01(float value) { return Math.max(0.0f, Math.min(1.0f, value)); }
}
