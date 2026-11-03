package dev.sqwat.vela.opt.feature.blocks;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "sign_cache",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.AbstractSignRendererMixin", "block.SignRendererMixin", "block.HangingSignRendererMixin"},
	bench = BenchView.BLOCKS
)
public class SignCacheFeature extends Feature {
}
