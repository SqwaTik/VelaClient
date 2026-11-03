package dev.sqwat.vela.opt.feature.ui;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "bidi_cache",
	group = FeatureGroup.INTERFACE,
	mixins = "text.ClientLanguageMixin",
	bench = BenchView.HOLOGRAMS
)
public class BidiCacheFeature extends Feature {
}
