// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.block;

import net.minecraft.world.level.block.state.BlockState;

public interface CachedValidity {
	BlockState zakoopt$validState();

	boolean zakoopt$valid();

	void zakoopt$setValid(BlockState state, boolean valid);

	long zakoopt$distanceTick();

	boolean zakoopt$inDistance();

	void zakoopt$setInDistance(boolean inDistance, long tick);
}
