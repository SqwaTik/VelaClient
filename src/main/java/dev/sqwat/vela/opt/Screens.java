package dev.sqwat.vela.opt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class Screens {
	private Screens() {}

	public static void setScreen(Screen screen) {
		Minecraft.getInstance().gui.setScreen(screen);
	}
}
