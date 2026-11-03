package dev.sqwat.vela.opt.feature.blocks;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "lookup_caches",
	group = FeatureGroup.BLOCKS,
	mixins = {"gl.RenderTypesMixin", "block.PistonHeadRendererMixin"},
	bench = BenchView.OVERVIEW
)
public class LookupCachesFeature extends Feature {
}
