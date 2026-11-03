package dev.sqwat.vela.opt.mixin.block;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.sqwat.vela.opt.block.CachedLight;
import dev.sqwat.vela.opt.block.CachedValidity;
import dev.sqwat.vela.opt.block.ChestFunctions;
import dev.sqwat.vela.opt.block.MovingSlots;
import dev.sqwat.vela.opt.block.TickCached;
import dev.sqwat.vela.opt.entity.PooledState;

@Getter
@Accessors(fluent = true, chain = false)
@Mixin(BlockEntity.class)
public class BlockEntityMixin implements TickCached, CachedLight, CachedValidity, PooledState, ChestFunctions, MovingSlots {
	@Unique
	private Object velaopt$moving0;
	@Unique
	private Object velaopt$moving1;
	@Unique
	private Object velaopt$chestOpen;
	@Unique
	private Object velaopt$chestLight;
	@Unique
	private long velaopt$chestTick = Long.MIN_VALUE;
	@Unique
	private Object velaopt$state;
	@Unique
	private Object velaopt$stateOwner;
	@Unique
	private Object velaopt$cached;
	@Unique
	private long velaopt$cachedTick = Long.MIN_VALUE;
	@Unique
	private int velaopt$light;
	@Unique
	private BlockState velaopt$validState;
	@Unique
	private boolean velaopt$valid;
	@Unique
	private long velaopt$lightTick = Long.MIN_VALUE;
	@Unique
	private long velaopt$distanceTick = Long.MIN_VALUE;
	@Unique
	private boolean velaopt$inDistance;
	@Unique
	private int velaopt$pairLight;
	@Unique
	private long velaopt$pairLightTick = Long.MIN_VALUE;

	@Override
	public Object velaopt$moving(int slot) {
		return slot == 0 ? velaopt$moving0 : velaopt$moving1;
	}

	@Override
	public void velaopt$setMoving(int slot, Object state) {
		if (slot == 0) {
			velaopt$moving0 = state;
		} else {
			velaopt$moving1 = state;
		}
	}

	@Override
	public Object velaopt$openFn(long tick) {
		return velaopt$chestTick == tick ? velaopt$chestOpen : null;
	}

	@Override
	public Object velaopt$lightFn(long tick) {
		return velaopt$chestTick == tick ? velaopt$chestLight : null;
	}

	@Override
	public void velaopt$setOpenFn(Object fn, long tick) {
		if (velaopt$chestTick != tick) {
			velaopt$chestTick = tick;
			velaopt$chestLight = null;
		}
		velaopt$chestOpen = fn;
	}

	@Override
	public void velaopt$setLightFn(Object fn, long tick) {
		if (velaopt$chestTick != tick) {
			velaopt$chestTick = tick;
			velaopt$chestOpen = null;
		}
		velaopt$chestLight = fn;
	}

	@Override
	public void velaopt$setState(Object state, Object owner) {
		velaopt$state = state;
		velaopt$stateOwner = owner;
	}

	@Override
	public void velaopt$setCached(Object value, long tick) {
		velaopt$cached = value;
		velaopt$cachedTick = tick;
	}

	@Override
	public void velaopt$setLight(int light, long tick) {
		velaopt$light = light;
		velaopt$lightTick = tick;
	}

	@Override
	public void velaopt$setPairLight(int light, long tick) {
		velaopt$pairLight = light;
		velaopt$pairLightTick = tick;
	}

	@Override
	public void velaopt$setValid(BlockState state, boolean valid) {
		velaopt$validState = state;
		velaopt$valid = valid;
	}

	@Override
	public void velaopt$setInDistance(boolean inDistance, long tick) {
		velaopt$inDistance = inDistance;
		velaopt$distanceTick = tick;
	}
}
