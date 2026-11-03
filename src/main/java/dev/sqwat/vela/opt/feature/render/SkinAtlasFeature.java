package dev.sqwat.vela.opt.feature.render;

import dev.sqwat.vela.opt.MixinPlugin;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;

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
