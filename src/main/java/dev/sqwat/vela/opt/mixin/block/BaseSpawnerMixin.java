package dev.sqwat.vela.opt.mixin.block;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.sqwat.vela.opt.block.TickCached;

@Getter
@Accessors(fluent = true, chain = false)
@Mixin(BaseSpawner.class)
public class BaseSpawnerMixin implements TickCached {
	@Unique
	private Object velaopt$cached;
	@Unique
	private long velaopt$cachedTick = Long.MIN_VALUE;

	@Override
	public void velaopt$setCached(Object value, long tick) {
		velaopt$cached = value;
		velaopt$cachedTick = tick;
	}
}
