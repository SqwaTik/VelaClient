package dev.sqwat.vela.opt.entity;

import net.minecraft.network.chat.Component;

public interface CachedName {
	Component velaopt$name();

	long velaopt$nameTick();

	void velaopt$setName(Component name, long tick);
}
