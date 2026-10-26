// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.feature;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;

// a block of features under one heading, in this order
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum FeatureGroup {
	RENDER("render"),
	ITEMS("items"),
	SPAWNERS("spawners"),
	BLOCKS("blocks"),
	PARTICLES("particles"),
	INTERFACE("interface"),
	MEMORY("memory");

	private final String key;

	public Component title() {
		return Component.translatable("zakoopt.group." + key);
	}

	public Component description() {
		return Component.translatable("zakoopt.group." + key + ".description");
	}
}
