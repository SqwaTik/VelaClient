package dev.sqwat.vela.opt.feature.ui;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "prepared_text_cache",
	group = FeatureGroup.INTERFACE,
	mixins = "text.FontMixin",
	bench = BenchView.HOLOGRAMS
)
public class PreparedTextCacheFeature extends Feature {
}
