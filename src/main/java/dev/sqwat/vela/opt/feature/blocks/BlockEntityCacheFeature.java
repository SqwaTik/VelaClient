package dev.sqwat.vela.opt.feature.blocks;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "block_entity_cache",
	group = FeatureGroup.BLOCKS,
	mixins = "block.ChestRendererMixin",
	bench = BenchView.BLOCKS
)
public class BlockEntityCacheFeature extends Feature {
}
