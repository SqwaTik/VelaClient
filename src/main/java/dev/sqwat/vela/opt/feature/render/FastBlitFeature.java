package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "fast_blit",
	group = FeatureGroup.RENDER,
	mixins = "gl.RenderTargetBlitMixin",
	bench = BenchView.OVERVIEW
)
public class FastBlitFeature extends Feature {
}
