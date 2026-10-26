// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.feature.Features;
import dev.sqwat.vela.zako.feature.Range;
import dev.sqwat.vela.zako.feature.Setting;

// Iris renders the world through its own targets, which would stay at full size
@FeatureData(
	key = "render_scale",
	group = FeatureGroup.RENDER,
	enabled = false,
	mixins = "gl.GameRendererScaleMixin"
)
public class RenderScaleFeature extends Feature {
	@Setting(key = "render_scale_percent")
	@Range(min = 50, max = 90, unit = "zakoopt.percent")
	public int percent = 75;

	@Override
	public String blocked() {
		return Features.shaders() ? "Iris" : null;
	}
}
