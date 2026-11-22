package dev.sqwat.vela.opt.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import lombok.experimental.UtilityClass;

// 1.21.4 render targets hand out their GL ids directly
@UtilityClass
public class GlTargets {
	// a pass that samples on its own takes the unit's sampler object off and puts it back (none is bound on 1.21.4: a no-op)
	public final boolean SAMPLERS = true;

	public int depth(RenderTarget target) {
		return target.getDepthTextureId();
	}
}
