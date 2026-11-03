package dev.sqwat.vela.opt.feature.spawners;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "spawner_tick_cache",
	group = FeatureGroup.SPAWNERS,
	mixins = "block.SpawnerRendererMixin",
	bench = BenchView.BLOCKS
)
public class SpawnerTickCacheFeature extends Feature {
}
