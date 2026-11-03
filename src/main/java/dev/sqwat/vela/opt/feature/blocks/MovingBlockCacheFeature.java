package dev.sqwat.vela.opt.feature.blocks;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "moving_block_cache",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.BlockFeatureRendererMixin", "block.PistonHeadRendererCacheMixin"},
	bench = BenchView.PISTONS
)
public class MovingBlockCacheFeature extends Feature {
}
