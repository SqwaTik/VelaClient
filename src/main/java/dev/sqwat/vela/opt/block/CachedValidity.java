package dev.sqwat.vela.opt.block;

import net.minecraft.world.level.block.state.BlockState;

public interface CachedValidity {
	BlockState velaopt$validState();

	boolean velaopt$valid();

	void velaopt$setValid(BlockState state, boolean valid);

	long velaopt$distanceTick();

	boolean velaopt$inDistance();

	void velaopt$setInDistance(boolean inDistance, long tick);
}
