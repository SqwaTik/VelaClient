package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "fbo_share",
	group = FeatureGroup.RENDER,
	mixins = {"gl.GlCommandEncoderDepthClearMixin", "gl.GlTextureViewMixin"},
	bench = BenchView.OVERVIEW
)
public class FboShareFeature extends Feature {
}
