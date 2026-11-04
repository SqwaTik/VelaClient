package dev.sqwat.vela.opt.gl;

import dev.sqwat.vela.opt.feature.Features;

// 1.21.5's GpuFence is a class the encoder hands out itself, with no createFence hook to share one per frame: vanilla fences
public final class FrameFence {
	private FrameFence() {}

	public static boolean enabled() {
		return Features.FRAME_FENCE.on;
	}

	public static void endFrame() {}
}
