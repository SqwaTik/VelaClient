package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Features;

// Iris draws terrain again for its shadow pass with another camera, which the verdicts would not fit
@FeatureData(
	key = "occlusion_cull",
	group = FeatureGroup.RENDER,
	enabled = false,
	mixins = "gl.SodiumOcclusionCullMixin"
)
public class OcclusionCullFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
