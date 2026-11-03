package dev.sqwat.vela.compat;

import org.joml.Vector3f;
import org.joml.Vector3fc;

/** 26.3 keeps the sky color as a float vector instead of a packed RGB int. */
public final class Colors {
    private Colors() {}
    public static int rgb(Vector3fc c) {
        return 0xFF000000 | channel(c.x()) << 16 | channel(c.y()) << 8 | channel(c.z());
    }
    public static Vector3f vec(int rgb) {
        return new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f);
    }
    private static int channel(float v) { return Math.clamp(Math.round(v * 255f), 0, 255); }
}
