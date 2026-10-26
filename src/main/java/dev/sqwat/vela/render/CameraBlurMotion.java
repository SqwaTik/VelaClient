package dev.sqwat.vela.render;

/** Camera exposure expressed in screen fractions, independent of the rendered frame rate. */
public final class CameraBlurMotion {
    private CameraBlurMotion() {}
    public static float rotation(float previous, float current, double seconds, float fov, float strength) {
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds > .1 || !Float.isFinite(fov) || fov <= 0) return 0;
        float delta = (current - previous) % 360;
        if (delta > 180) delta -= 360;
        if (delta < -180) delta += 360;
        return (float)Math.clamp(delta / seconds * .012 * strength / fov, -.08, .08);
    }
    public static int encode(float fraction) {
        return Math.clamp(Math.round((fraction / .08f * .5f + .5f) * 254), 0, 254);
    }
}
