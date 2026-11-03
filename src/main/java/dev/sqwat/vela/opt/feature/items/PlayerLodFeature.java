package dev.sqwat.vela.opt.feature.items;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Range;
import dev.sqwat.vela.opt.feature.Setting;

@FeatureData(
	key = "player_lod",
	visual = true,
	group = FeatureGroup.ITEMS,
	mixins = {"entity.AvatarLodMixin", "entity.PlayerLodMixin"}
)
public class PlayerLodFeature extends Feature {
	@Setting(key = "player_lod_distance")
	@Range(min = 8, max = 96)
	public int distance = 35;
}
