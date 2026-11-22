package dev.sqwat.vela.opt.gl;

// the automatic render scale: the world keeps full resolution while the frame rate holds the target and drops in 5% steps
// only when it does not, down to the chosen lowest share. It climbs back once the frame rate at the next step up, estimated
// from the pixel count, would still hold the target. A step down that brings no frames back means the GPU is not the limit
// (the CPU is, or a frame rate cap): it is undone and not tried again for a while
public final class AutoScale {
	private static final long WINDOW = 500_000_000L, HOLD = 15_000_000_000L;
	private static final int STEP = 5;
	private int current = 100;
	private long windowStart = -1, holdUntil;
	private int frames;
	private double before;

	// called once per world frame; the share of the window resolution to draw at, 100 for the full one
	public int percent(long now, int lowest, int target) {
		current = Math.max(current, lowest);
		if (windowStart < 0) {
			windowStart = now;
			return current;
		}
		frames++;
		if (now - windowStart < WINDOW) {
			return current;
		}
		double fps = frames * 1e9 / (now - windowStart);
		windowStart = now;
		frames = 0;
		if (before > 0) {
			boolean helped = fps >= before * 1.03;
			before = 0;
			if (!helped) {
				current = Math.min(100, current + STEP);
				holdUntil = now + HOLD;
				return current;
			}
		}
		if (fps < target * 0.95 && current > lowest && now >= holdUntil) {
			before = fps;
			current = Math.max(lowest, current - STEP);
		} else if (fps > target * 1.1 && current < 100) {
			double ratio = (double) current / (current + STEP);
			if (fps * ratio * ratio >= target * 1.02) {
				current = Math.min(100, current + STEP);
			}
		}
		return current;
	}

	public void reset() {
		current = 100;
		windowStart = -1;
		frames = 0;
		before = 0;
		holdUntil = 0;
	}
}
