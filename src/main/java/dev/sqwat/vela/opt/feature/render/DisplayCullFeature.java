package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "display_cull",
	group = FeatureGroup.RENDER,
	mixins = "entity.DisplayCullMixin",
	bench = BenchView.CROWD
)
public class DisplayCullFeature extends Feature {
}
