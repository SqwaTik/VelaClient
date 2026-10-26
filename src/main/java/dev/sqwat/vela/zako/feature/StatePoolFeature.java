// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import dev.sqwat.vela.zako.BenchView;
import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.feature.Features;

// the Iris shadow pass extracts the same entities again for its own camera, which would overwrite the pooled state
@FeatureData(
	key = "state_pool",
	group = FeatureGroup.MEMORY,
	mixins = "entity.EntityRenderStatePoolMixin",
	bench = BenchView.CROWD
)
public class StatePoolFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
