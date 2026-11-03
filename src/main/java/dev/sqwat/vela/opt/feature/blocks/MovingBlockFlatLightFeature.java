package dev.sqwat.vela.opt.feature.blocks;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "moving_block_flat_light",
	visual = true,
	group = FeatureGroup.BLOCKS,
	mixins = "block.NonTerrainBlockRenderContextMixin"
)
public class MovingBlockFlatLightFeature extends Feature {
}
