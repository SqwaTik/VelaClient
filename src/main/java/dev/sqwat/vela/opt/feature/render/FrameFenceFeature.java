package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "frame_fence",
	group = FeatureGroup.RENDER,
	mixins = "gl.GlCommandEncoderMixin",
	bench = BenchView.OVERVIEW
)
public class FrameFenceFeature extends Feature {
}
