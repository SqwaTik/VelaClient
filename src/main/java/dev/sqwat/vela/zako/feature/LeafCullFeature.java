// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.Features;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;

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
