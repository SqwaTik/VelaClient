package dev.sqwat.vela.opt.feature.items;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "item_single_copy",
	visual = true,
	group = FeatureGroup.ITEMS,
	enabled = false,
	mixins = {"entity.ItemClusterRenderStateMixin", "entity.ItemEntityRendererMixin"}
)
public class ItemSingleCopyFeature extends Feature {
}
