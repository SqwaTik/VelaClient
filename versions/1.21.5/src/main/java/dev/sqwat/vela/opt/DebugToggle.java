package dev.sqwat.vela.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

// F3 on 1.21.5: the overlay itself, not 1.21.9's debug entry list
@UtilityClass
public class DebugToggle {
	public void toggle() {
		Minecraft.getInstance().getDebugOverlay().toggleOverlay();
	}

	public boolean visible() {
		return Minecraft.getInstance().getDebugOverlay().showDebugScreen();
	}
}
