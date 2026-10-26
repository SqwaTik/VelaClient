// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import dev.sqwat.vela.zako.BenchView;
import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;

@FeatureData(
	key = "block_entity_early_cull",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.BlockEntityRenderDispatcherCullMixin", "block.BlockEntityRenderDispatcherCullWrapMixin"},
	bench = BenchView.BLOCKS
)
public class BlockEntityEarlyCullFeature extends Feature {
}
