package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "no_item_sort",
	visual = true,
	group = FeatureGroup.RENDER,
	mixins = "gl.BufferSourceMixin",
	bench = BenchView.ITEMS
)
public class NoItemSortFeature extends Feature {
}
