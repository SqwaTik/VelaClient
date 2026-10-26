// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature.blocks;

import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.feature.Features;

// vanilla objects made anew every frame are reused instead; the Iris shadow pass extracts the same block entities again
// for its own camera, which would overwrite a reused state or pose
@FeatureData(
	key = "fewer_allocations",
	group = FeatureGroup.MEMORY,
	mixins = {"block.BlockEntityRenderStatePoolMixin", "entity.SubmitPosePoolMixin", "gl.BatchableBufferSourceSetPoolMixin", "entity.ModelPartChildrenMixin"}
)
public class FewerAllocationsFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
