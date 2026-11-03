package dev.sqwat.vela.opt.feature.ui;

import dev.sqwat.vela.opt.MixinPlugin;
import dev.sqwat.vela.opt.OptConfig;
import dev.sqwat.vela.opt.bench.BenchView;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.FeatureData;
import dev.sqwat.vela.opt.feature.FeatureGroup;
import dev.sqwat.vela.opt.hud.HudWorth;

import java.util.Objects;

@FeatureData(
	key = "hud_cache",
	group = FeatureGroup.INTERFACE,
	mixins = {"hud.GuiRendererMixin", "hud.GuiRendererTargetMixin", "hud.GuiHudMixin"},
	bench = BenchView.OVERVIEW
)
public class HudCacheFeature extends Feature {
	@Override
	public String blocked() {
		return MixinPlugin.VULKAN ? "VulkanMod" : OptConfig.FOREIGN_HUD ? OptConfig.FOREIGN_HUD_MOD
				: HudWorth.isMainSwapped() ? Objects.requireNonNullElse(HudWorth.getSwappedBy(), "velaopt.blocked.other_mod") : null;
	}
}
