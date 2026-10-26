// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import dev.sqwat.vela.zako.MixinPlugin;
import dev.sqwat.vela.zako.ZakoOptConfig;
import dev.sqwat.vela.zako.BenchView;
import dev.sqwat.vela.zako.feature.Feature;
import dev.sqwat.vela.zako.feature.FeatureData;
import dev.sqwat.vela.zako.feature.FeatureGroup;
import dev.sqwat.vela.zako.hud.HudWorth;

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
		return MixinPlugin.VULKAN ? "VulkanMod" : ZakoOptConfig.FOREIGN_HUD ? ZakoOptConfig.FOREIGN_HUD_MOD
				: HudWorth.isMainSwapped() ? Objects.requireNonNullElse(HudWorth.getSwappedBy(), "zakoopt.blocked.other_mod") : null;
	}
}
