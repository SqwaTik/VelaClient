package dev.sqwat.vela.opt.feature.blocks;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.Features;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

// the faces are left out when chunks are meshed, so a change needs the chunks meshed again
@FeatureData(
	key = "leaf_cull",
	group = FeatureGroup.BLOCKS,
	mixins = {"block.SodiumLeafCullMixin", "block.SodiumLeafCullOldMixin"}
)
public class LeafCullFeature extends Feature {
	@Override
	public void refresh() {
		boolean was = on;
		super.refresh();
		if (was != on) {
			Features.terrainChanged = true;
		}
	}
}
