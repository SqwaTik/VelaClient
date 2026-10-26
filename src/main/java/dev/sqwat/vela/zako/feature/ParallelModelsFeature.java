// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import dev.sqwat.vela.zako.ZakoOptConfig;
import dev.sqwat.vela.zako.BenchView;
import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.feature.Features;

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
		return ZakoOptConfig.CORES < 4 ? "zakoopt.blocked.cpu" : Features.shaders() ? "Iris" : null;
	}
}
