package dev.sqwat.vela.opt.feature.items;

import dev.sqwat.vela.opt.OptConfig;
import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Features;

// Iris builds its extended entity attributes from the vertices it is handed one by one
@FeatureData(
	key = "parallel_models",
	group = FeatureGroup.ITEMS,
	mixins = {"entity.ModelParallelMixin", "entity.ModelFeatureRendererMixin"},
	bench = BenchView.CROWD
)
public class ParallelModelsFeature extends Feature {
	@Override
	public String blocked() {
		return OptConfig.CORES < 4 ? "velaopt.blocked.cpu" : Features.shaders() ? "Iris" : null;
	}
}
