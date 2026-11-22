package dev.sqwat.vela.opt.bench;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;

// the benchmark world on 1.21.5: leaving it is the pause screen's own disconnect
@UtilityClass
public class BenchWorld {
	public final boolean LEGACY_SNBT = false;

	public LevelSettings settings(String name) {
		return new LevelSettings(name, GameType.CREATIVE, false, Difficulty.EASY, true, new GameRules(FeatureFlags.DEFAULT_FLAGS), WorldDataConfiguration.DEFAULT);
	}

	public void leave(Minecraft mc) {
		if (mc.level != null) {
			mc.level.disconnect();
		}
		mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
	}
}
