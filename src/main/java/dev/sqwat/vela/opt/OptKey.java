package dev.sqwat.vela.opt;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

@UtilityClass
public class OptKey {
	public final KeyMapping MONITOR = new KeyMapping("key.velaopt.monitor", InputConstants.KEY_F6,
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("velaopt", "velaopt")));

	public boolean shift() {
		return Minecraft.getInstance().hasShiftDown();
	}
}
