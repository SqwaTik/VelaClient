package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Features;
import dev.sqwat.vela.opt.feature.Range;
import dev.sqwat.vela.opt.feature.Setting;
import dev.sqwat.vela.opt.gl.AutoScale;

// Iris renders the world through its own targets, which would stay at full size
@FeatureData(
	key = "render_scale",
	group = FeatureGroup.RENDER,
	enabled = false,
	mixins = "gl.GameRendererScaleMixin"
)
public class RenderScaleFeature extends Feature {
	@Setting(key = "render_scale_percent")
	@Range(min = 50, max = 90, unit = "velaopt.percent")
	public int percent = 75;

	// keeps full resolution while the frame rate holds the target; "percent" is then the lowest it goes
	@Setting(key = "render_scale_auto")
	public boolean auto = true;

	@Setting(key = "render_scale_target")
	@Range(min = 30, max = 240, unit = "velaopt.fps")
	public int target = 60;

	private final AutoScale automatic = new AutoScale();

	// this frame's share of the window resolution
	public int scale() {
		if (!auto) {
			automatic.reset();
			return percent;
		}
		return automatic.percent(System.nanoTime(), percent, target);
	}

	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
