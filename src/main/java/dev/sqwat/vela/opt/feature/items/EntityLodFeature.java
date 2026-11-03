package dev.sqwat.vela.opt.feature.items;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Range;
import dev.sqwat.vela.opt.feature.Setting;

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
