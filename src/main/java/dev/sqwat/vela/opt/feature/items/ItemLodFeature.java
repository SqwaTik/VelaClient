package dev.sqwat.vela.opt.feature.items;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Range;
import dev.sqwat.vela.opt.feature.Setting;

@FeatureData(
	key = "item_lod",
	visual = true,
	group = FeatureGroup.ITEMS,
	mixins = {"entity.ItemRendererMixin", "entity.ItemFeatureRendererMixin"}
)
public class ItemLodFeature extends Feature {
	@Setting(key = "item_lod_distance")
	@Range(min = 4, max = 48)
	public int distance = 8;
}
