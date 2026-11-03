package dev.sqwat.vela.opt.feature.blocks;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "crack_cache",
	group = FeatureGroup.BLOCKS,
	mixins = "block.LevelRendererCrackMixin"
)
public class CrackCacheFeature extends Feature {
}
