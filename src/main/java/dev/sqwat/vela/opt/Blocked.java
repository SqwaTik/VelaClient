package dev.sqwat.vela.opt;

import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import dev.sqwat.vela.opt.feature.Feature;
import dev.sqwat.vela.opt.feature.Features;

// why an option is held off on this setup whatever its setting, worded for the options screens; null: it works as set
@UtilityClass
public class Blocked {
	public Component reason(String key) {
		Feature feature = Features.byKey(key);
		String cause = feature == null ? null : feature.blocked();
		return cause == null ? null : Component.translatable("velaopt.blocked",
				cause.startsWith("velaopt.") ? Component.translatable(cause) : Component.literal(cause));
	}
}
