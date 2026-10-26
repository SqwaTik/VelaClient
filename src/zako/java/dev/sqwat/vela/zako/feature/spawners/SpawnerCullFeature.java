// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature.spawners;

import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.feature.Range;
import dev.sqwat.vela.zako.feature.Setting;

@FeatureData(
	key = "spawner_cull",
	visual = true,
	group = FeatureGroup.SPAWNERS,
	mixins = "block.SpawnerRendererMixin"
)
public class SpawnerCullFeature extends Feature {
	@Setting(key = "spawner_distance")
	@Range(min = 4, max = 64)
	public int distance = 16;
}
