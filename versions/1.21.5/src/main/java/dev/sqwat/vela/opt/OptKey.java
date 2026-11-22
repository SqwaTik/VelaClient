package dev.sqwat.vela.opt;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;

// 1.21.5 keys sit in string categories, and the controls screen sorts by a fixed table of them: a vanilla one is safe
@UtilityClass
public class OptKey {
	public final KeyMapping MONITOR = new KeyMapping("key.velaopt.monitor", InputConstants.KEY_F6, "key.categories.misc");

	public boolean shift() {
		return Screen.hasShiftDown();
	}
}
