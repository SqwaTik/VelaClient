package dev.sqwat.vela.opt.feature.blocks;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "block_entity_early_cull",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.BlockEntityRenderDispatcherCullMixin", "block.BlockEntityRenderDispatcherCullWrapMixin", "block.NeoBlockEntityRenderDispatcherMixin"},
	bench = BenchView.BLOCKS
)
public class BlockEntityEarlyCullFeature extends Feature {
}
