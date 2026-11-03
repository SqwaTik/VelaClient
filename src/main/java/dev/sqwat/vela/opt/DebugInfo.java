package dev.sqwat.vela.opt;

import dev.sqwat.vela.opt.feature.Features;

import lombok.experimental.UtilityClass;
import dev.sqwat.vela.opt.gl.RingAutoTune;
import dev.sqwat.vela.opt.hud.HudWorth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@UtilityClass
public class DebugInfo {
	private List<OptOptions.Toggle> available;
	// more F3 lines from version code that has them to show
	public final List<Supplier<List<String>>> EXTRA = new CopyOnWriteArrayList<>();

	public List<String> lines() {
		List<String> lines = new ArrayList<>();
		lines.add(line());
		lines.addAll(Guard.lines());
		EXTRA.forEach(extra -> lines.addAll(extra.get()));
		return lines;
	}

	public String line() {
		// available() asks the class loader for a resource, so the list is resolved once
		if (available == null) {
			available = OptOptions.GROUPS.stream().flatMap(g -> g.options().stream())
					.filter(o -> o instanceof OptOptions.Toggle && o.available()).map(o -> (OptOptions.Toggle) o).toList();
		}
		long on = available.stream().filter(t -> t.get().get()).count();
		String ring = !Features.IMMEDIATE_RING.value ? "off" : Features.IMMEDIATE_RING.forced() ? (Features.IMMEDIATE_RING.on ? "on" : "off") + " (forced)"
				: OptConfig.RING_PRESENT ? RingAutoTune.status() : "n/a";
		String hud = MixinPlugin.VULKAN ? "off (VulkanMod)" : OptConfig.FOREIGN_HUD ? "off (" + OptConfig.FOREIGN_HUD_MOD + ")" : HudWorth.isMainSwapped() ? "off (main target swapped by " + Objects.requireNonNullElse(HudWorth.getSwappedBy(), "another mod") + ")" : !Features.HUD_CACHE.value ? "off" : HudWorth.isWorth() ? "on" : "idle (FPS too close to monitor rate)";
		if (!OptConfig.enabled) {
			return "Vela " + OptOptions.VERSION + ": off";
		}
		return "Vela " + OptOptions.VERSION + ": " + on + "/" + available.size() + " on, ring " + ring + ", HUD cache " + hud + ", cores " + dev.sqwat.vela.opt.cpu.CorePin.status();
	}
}
