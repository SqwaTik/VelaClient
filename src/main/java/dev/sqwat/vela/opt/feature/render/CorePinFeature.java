package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.cpu.CorePin;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

@FeatureData(
	key = "core_pin",
	group = FeatureGroup.RENDER,
	enabled = false,
	bench = BenchView.CROWD
)
public class CorePinFeature extends Feature {
	@Override
	public String blocked() {
		return CorePin.available() ? null : "velaopt.blocked.cores_same";
	}

	// refresh runs on the render thread, which moves itself here
	@Override
	public void refresh() {
		super.refresh();
		CorePin.set(on);
		CorePin.check();
	}
}
