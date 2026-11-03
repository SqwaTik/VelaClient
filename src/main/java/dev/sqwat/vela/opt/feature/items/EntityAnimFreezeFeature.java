package dev.sqwat.vela.opt.feature.items;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Range;
import dev.sqwat.vela.opt.feature.Setting;

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
