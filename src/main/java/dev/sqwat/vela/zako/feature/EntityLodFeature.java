// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.feature.Range;
import dev.sqwat.vela.zako.feature.Setting;

@FeatureData(
	key = "entity_lod",
	visual = true,
	group = FeatureGroup.ITEMS,
	mixins = {"entity.LivingEntityLodMixin", "entity.AvatarLodMixin", "entity.PlayerLodMixin"}
)
public class EntityLodFeature extends Feature {
	@Setting(key = "entity_lod_distance")
	@Range(min = 8, max = 96)
	public int distance = 35;
}
