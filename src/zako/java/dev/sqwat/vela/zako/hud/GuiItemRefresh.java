// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.hud;

import dev.sqwat.vela.zako.feature.Features;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GuiItemRefresh {
	private long last;
	private boolean refresh = true;

	public void decide() {
		long now = System.nanoTime();
		// the HUD cache already redraws at the monitor rate; throttling on top of it left enchanted hotbar items blank
		refresh = !Features.GUI_ANIMATED_ITEMS.on || HudCache.active() || now - last >= MonitorRate.intervalNs(now);
		if (refresh) {
			last = now;
		}
	}

	public boolean refresh() {
		return refresh;
	}
}
