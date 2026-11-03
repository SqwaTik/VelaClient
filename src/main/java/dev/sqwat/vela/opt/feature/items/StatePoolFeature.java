package dev.sqwat.vela.opt.feature.items;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Features;

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
