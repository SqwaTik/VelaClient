// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature.blocks;

import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.Features;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.feature.Range;
import dev.sqwat.vela.zako.feature.Setting;

// switching it on or off needs the chunks meshed again; a new distance is picked up as the camera moves
@FeatureData(
	key = "far_leaves",
	visual = true,
	group = FeatureGroup.BLOCKS,
	enabled = false,
	mixins = "block.SodiumFarLeavesMixin"
)
public class FarLeavesFeature extends Feature {
	@Setting(key = "far_leaves_distance")
	@Range(min = 16, max = 128)
	public int distance = 32;

	@Override
	public void refresh() {
		boolean was = on;
		super.refresh();
		if (was != on) {
			Features.terrainChanged = true;
		}
	}
}
