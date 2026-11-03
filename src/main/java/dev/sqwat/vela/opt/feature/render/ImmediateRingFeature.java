package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.OptConfig;
import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Features;
import dev.sqwat.vela.opt.feature.Setting;
import dev.sqwat.vela.opt.gl.RingAutoTune;

// on its own the ring stays only if the auto-tune measured it faster; "always" skips that measurement
@FeatureData(
	key = "immediate_ring",
	group = FeatureGroup.RENDER,
	mixins = {"gl.BatchableBufferSourceMixin", "gl.BufferUploaderMixin"},
	bench = BenchView.OVERVIEW
)
public class ImmediateRingFeature extends Feature {
	@Setting(key = "immediate_ring_force")
	public boolean force = false;

	public boolean forced() {
		return System.getProperty(property()) != null || force;
	}

	@Override
	public void refresh() {
		boolean forced = forced();
		if (Features.active && value && !forced && OptConfig.RING_PRESENT) {
			RingAutoTune.frame();
		}
		on = flag(value) && (forced || RingAutoTune.ringAllowed());
	}
}
