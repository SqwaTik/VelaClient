package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "lunar_multidraw",
	group = FeatureGroup.RENDER,
	config = "lunarMultiDraw",
	mixins = {"gl.LunarMultiDrawMixin", "gl.LunarSodiumMultiDrawMixin"},
	bench = BenchView.TERRAIN
)
public class LunarMultiDrawFeature extends Feature {
}
