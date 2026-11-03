package dev.sqwat.vela.opt.feature.spawners;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Range;
import dev.sqwat.vela.opt.feature.Setting;

@FeatureData(
	key = "spawner_cull",
	visual = true,
	group = FeatureGroup.SPAWNERS,
	mixins = "block.SpawnerRendererMixin"
)
public class SpawnerCullFeature extends Feature {
	@Setting(key = "spawner_distance")
	@Range(min = 4, max = 64)
	public int distance = 16;
}
