// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import dev.sqwat.vela.zako.ZakoOptConfig;
import dev.sqwat.vela.zako.BenchView;
import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.feature.Features;
import dev.sqwat.vela.zako.feature.Setting;
import dev.sqwat.vela.zako.gl.RingAutoTune;

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
		super.refresh();
		if (!on || !ZakoOptConfig.RING_PRESENT) { on = false; return; }
		boolean forced = forced();
		if (!forced) {
			RingAutoTune.frame();
		}
		on = forced || RingAutoTune.ringAllowed();
	}
}
