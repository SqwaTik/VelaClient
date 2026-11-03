package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "no_entity_sort",
	group = FeatureGroup.RENDER,
	mixins = "gl.BufferSourceMixin",
	bench = BenchView.CROWD
)
public class NoEntitySortFeature extends Feature {
}
