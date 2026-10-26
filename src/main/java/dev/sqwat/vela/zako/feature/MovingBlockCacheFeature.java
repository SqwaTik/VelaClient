// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import dev.sqwat.vela.zako.BenchView;
import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;

@FeatureData(
	key = "moving_block_cache",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.BlockFeatureRendererMixin", "block.PistonHeadRendererCacheMixin"},
	bench = BenchView.PISTONS
)
public class MovingBlockCacheFeature extends Feature {
}
