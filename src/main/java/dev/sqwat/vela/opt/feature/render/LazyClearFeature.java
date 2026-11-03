package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "lazy_clear",
	group = FeatureGroup.RENDER,
	mixins = "gl.GlCommandEncoderClearMixin",
	bench = BenchView.OVERVIEW
)
public class LazyClearFeature extends Feature {
}
