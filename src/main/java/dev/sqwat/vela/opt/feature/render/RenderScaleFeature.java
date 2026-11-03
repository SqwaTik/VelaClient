package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.feature.Features;
import dev.sqwat.vela.opt.feature.Range;
import dev.sqwat.vela.opt.feature.Setting;

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

	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
