package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Features;

// Iris reads vertices back to build its extended attributes, and reads from the mapped ring are uncached
@FeatureData(
	key = "ring_zero_copy",
	group = FeatureGroup.RENDER,
	mixins = {"gl.BatchableBufferSourceMixin", "gl.BufferUploaderMixin"},
	bench = BenchView.OVERVIEW
)
public class RingZeroCopyFeature extends Feature {
	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
