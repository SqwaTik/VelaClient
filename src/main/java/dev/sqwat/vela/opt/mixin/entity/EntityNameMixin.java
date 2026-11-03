package dev.sqwat.vela.opt.mixin.entity;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.sqwat.vela.opt.entity.CachedName;
import dev.sqwat.vela.opt.entity.CachedRenderer;
import dev.sqwat.vela.opt.entity.PooledState;

@Getter
@Accessors(fluent = true, chain = false)
@Mixin(Entity.class)
public class EntityNameMixin implements CachedName, CachedRenderer, PooledState {
	@Unique
	private Component velaopt$name;
	@Unique
	private long velaopt$nameTick = Long.MIN_VALUE;
	@Unique
	private Object velaopt$renderer;
	@Unique
	private Object velaopt$rendererMap;
	@Unique
	private Object velaopt$state;
	@Unique
	private Object velaopt$stateOwner;

	@Override
	public void velaopt$setName(Component name, long tick) {
		velaopt$name = name;
		velaopt$nameTick = tick;
	}

	@Override
	public void velaopt$setRenderer(Object renderer, Object map) {
		velaopt$renderer = renderer;
		velaopt$rendererMap = map;
	}

	@Override
	public void velaopt$setState(Object state, Object owner) {
		velaopt$state = state;
		velaopt$stateOwner = owner;
	}
}
