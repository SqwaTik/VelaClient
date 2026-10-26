// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

// a line in the player's chat, only on this side
@UtilityClass
public class Chat {
	public boolean ready() {
		return Minecraft.getInstance().player != null;
	}

	public void show(Component text) {
		Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(text);
	}
}
