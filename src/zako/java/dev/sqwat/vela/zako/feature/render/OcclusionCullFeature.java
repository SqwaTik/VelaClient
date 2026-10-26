// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature.render;

import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.feature.Features;

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
