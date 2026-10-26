package dev.sqwat.vela.ui.animation;

public final class FrameClock {
    private long previous = System.nanoTime();
    public void reset(){previous=System.nanoTime();}

    public float step() {
        long now = System.nanoTime();
        float delta = Math.min((now - previous) / 1_000_000_000.0f, 0.05f);
        previous = now;
        return Math.max(delta, 0.0f);
    }
}
