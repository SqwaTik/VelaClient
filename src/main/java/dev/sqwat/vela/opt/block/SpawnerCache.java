package dev.sqwat.vela.opt.block;

import dev.sqwat.vela.opt.feature.Features;

import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.level.BaseSpawner;

@UtilityClass
public class SpawnerCache {

	public boolean enabled() {
		return Features.SPAWNER_TICK_CACHE.on;
	}

	public EntityRenderState get(BaseSpawner spawner, long tick) {
		TickCached c = (TickCached) spawner;
		return c.velaopt$cachedTick() == tick ? (EntityRenderState) c.velaopt$cached() : null;
	}

	public void put(BaseSpawner spawner, long tick, EntityRenderState state) {
		((TickCached) spawner).velaopt$setCached(state, tick);
	}
}
