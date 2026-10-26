// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import dev.sqwat.vela.zako.MixinPlugin;
import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;

@FeatureData(
	key = "skin_atlas",
	group = FeatureGroup.RENDER,
	mixins = "entity.LivingEntityRendererSkinMixin"
)
public class SkinAtlasFeature extends Feature {
	@Override
	public String blocked() {
		return MixinPlugin.VULKAN ? "VulkanMod" : null;
	}
}
