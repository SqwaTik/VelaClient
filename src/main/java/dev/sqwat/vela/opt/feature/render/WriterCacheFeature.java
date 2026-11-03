package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "writer_cache",
	group = FeatureGroup.RENDER,
	mixins = "gl.VertexConsumerUtilsMixin",
	bench = BenchView.CROWD
)
public class WriterCacheFeature extends Feature {
}
