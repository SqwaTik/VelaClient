// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature.render;

import dev.sqwat.vela.zako.bench.BenchView;
import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;

@FeatureData(
	key = "no_entity_sort",
	group = FeatureGroup.RENDER,
	mixins = "gl.BufferSourceMixin",
	bench = BenchView.CROWD
)
public class NoEntitySortFeature extends Feature {
}
