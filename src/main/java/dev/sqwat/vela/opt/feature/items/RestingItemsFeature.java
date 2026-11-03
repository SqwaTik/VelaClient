package dev.sqwat.vela.opt.feature.items;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "resting_items",
	group = FeatureGroup.ITEMS,
	mixins = "entity.ItemEntityRestMixin",
	bench = BenchView.ITEMS
)
public class RestingItemsFeature extends Feature {
}
