// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import dev.sqwat.vela.zako.BenchView;
import dev.sqwat.vela.zako.CorePin;
import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;

@FeatureData(
	key = "core_pin",
	group = FeatureGroup.RENDER,
	enabled = false,
	bench = BenchView.CROWD
)
public class CorePinFeature extends Feature {
	@Override
	public String blocked() {
		return CorePin.available() ? null : "zakoopt.blocked.cores_same";
	}

	// refresh runs on the render thread, which moves itself here
	@Override
	public void refresh() {
		super.refresh();
		CorePin.set(on);
		CorePin.check();
	}
}
