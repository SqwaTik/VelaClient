// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature.items;

import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.feature.Range;
import dev.sqwat.vela.zako.feature.Setting;

@FeatureData(
	key = "entity_anim_freeze",
	visual = true,
	group = FeatureGroup.ITEMS,
	enabled = false,
	mixins = {"entity.ModelFeatureRendererMixin", "entity.LivingEntityAnimFreezeMixin"}
)
public class EntityAnimFreezeFeature extends Feature {
	@Setting(key = "entity_anim_distance")
	@Range(min = 4, max = 96)
	public int distance = 16;
}
