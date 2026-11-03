package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Features;

@FeatureData(
	key = "outline_skip",
	group = FeatureGroup.RENDER,
	mixins = "entity.LevelRendererOutlineMixin",
	bench = BenchView.CROWD
)
public class OutlineSkipFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
