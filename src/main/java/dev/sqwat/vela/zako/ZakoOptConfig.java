// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako;

import java.net.URI;
import dev.sqwat.vela.zako.feature.Features;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;


@UtilityClass
@Slf4j(topic = "zakoopt")
public class ZakoOptConfig {
	public final URI CHANNEL = URI.create("https://t.me/StarikZako");
	public final boolean IRIS = FabricLoader.getInstance().isModLoaded("iris");
	// AxolotlClient draws its HUD straight to the screen, Exordium caches the HUD itself by swapping the main render target:
	// with our HUD cache either one flickers
	// mods that cache or spread the HUD over frames themselves: two such layers wash the HUD out
	public final boolean FOREIGN_HUD = FabricLoader.getInstance().isModLoaded("axolotlclient") || FabricLoader.getInstance().isModLoaded("exordium")
			|| FabricLoader.getInstance().isModLoaded("gnetum");
	public final boolean RING_PRESENT = ZakoOptConfig.class.getClassLoader().getResource("dev/sqwat/vela/zako/gl/ImmediateRing.class") != null && !MixinPlugin.VULKAN;
	public final int CORES = Runtime.getRuntime().availableProcessors();
	public final String FOREIGN_HUD_MOD = FabricLoader.getInstance().isModLoaded("axolotlclient") ? "AxolotlClient"
			: FabricLoader.getInstance().isModLoaded("exordium") ? "Exordium" : "Gnetum";

	public Values values = new Values();
	public boolean enabled;
	private boolean featuresReady;
	public final class Values {
		public boolean guiAnimatedItems = true;
		public boolean frameFence = true;
		public boolean lazyClear = true;
		public boolean noEntitySort = true;
		public boolean noItemSort = true;
		public boolean itemSingleCopy = false;
		public boolean bidiCache = true;
		public boolean preparedTextCache = true;
		public boolean itemLod = true;
		public int itemLodDistance = 8;
		public boolean entityLod = true;
		public int entityLodDistance = 35;
		public boolean playerLod = true;
		public boolean entityAnimFreeze = false;
		public int entityAnimDistance = 16;
		public boolean particleLight = true;
		public boolean particlePhysics = true;
		public boolean particleLod = true;
		public boolean parallelParticles = true;
		public boolean parallelModels = true;
		public boolean spawnerCull = true;
		public int spawnerDistance = 16;
		public boolean spawnerTickCache = true;
		public boolean spawnerReplay = true;
		public boolean movingBlockFlatLight = true;
		public boolean movingBlockCache = true;
		public boolean lookupCaches = true;
		public boolean microOpts = true;
		public boolean hudCache = true;
		public boolean skinAtlas = true;
		public boolean writerCache = true;
		public boolean signCache = true;
		public boolean fboShare = true;
		public boolean outlineSkip = true;
		public boolean ringZeroCopy = true;
		public boolean immediateRing = true;
		public boolean immediateRingForce = false;
		public boolean blockEntityCache = true;
		public boolean blockEntityEarlyCull = true;
		public boolean lunarMultiDraw = true;
		public boolean textureBuffer = true;
	}

	// One runtime registry owns compatibility, failure gates and feature state.
	public void refresh() {
		var module = dev.sqwat.vela.module.ZakoOptimization.active();
		enabled = module != null;
		if (module != null) values = module.values();
		if (module != null || featuresReady) {
			featuresReady = true;
			Features.refresh();
		}
	}

	public boolean guiAnimatedItems() {
		return Features.GUI_ANIMATED_ITEMS.on;
	}

	public boolean frameFence() {
		return false;
	}

	public boolean lazyClear() {
		return Features.LAZY_CLEAR.on;
	}

	public boolean noEntitySort() {
		return Features.NO_ENTITY_SORT.on;
	}

	public boolean noItemSort() {
		return Features.NO_ITEM_SORT.on;
	}

	public boolean itemSingleCopy() {
		return Features.ITEM_SINGLE_COPY.on;
	}

	public boolean bidiCache() {
		return Features.BIDI_CACHE.on;
	}

	public boolean preparedTextCache() {
		return Features.PREPARED_TEXT_CACHE.on;
	}

	public boolean itemLod() {
		return Features.ITEM_LOD.on;
	}

	public int itemLodDistance() {
		return Features.ITEM_LOD.distance;
	}

	public boolean entityLod() {
		return Features.ENTITY_LOD.on;
	}

	public int entityLodDistance() {
		return Features.ENTITY_LOD.distance;
	}

	public boolean playerLod() {
		return Features.PLAYER_LOD.on;
	}

	public boolean entityAnimFreeze() {
		return Features.ENTITY_ANIM_FREEZE.on;
	}

	public int entityAnimDistance() {
		return Features.ENTITY_ANIM_FREEZE.distance;
	}

	public boolean particleLight() {
		return Features.PARTICLE_LIGHT.on;
	}

	public boolean particlePhysics() {
		return Features.PARTICLE_PHYSICS.on;
	}

	public boolean particleLod() {
		return Features.PARTICLE_LOD.on;
	}

	public boolean parallelParticles() {
		return Features.PARALLEL_PARTICLES.on;
	}

	public boolean parallelParticleVertices() {
		return Features.PARALLEL_PARTICLES.vertices;
	}

	public boolean parallelModels() {
		return Features.PARALLEL_MODELS.on;
	}

	public boolean spawnerCull() {
		return Features.SPAWNER_CULL.on;
	}

	public int spawnerDistance() {
		return Features.SPAWNER_CULL.distance;
	}

	public boolean blockEntityCache() {
		return Features.BLOCK_ENTITY_CACHE.on;
	}

	public boolean blockEntityEarlyCull() {
		return Features.BLOCK_ENTITY_EARLY_CULL.on;
	}

	public boolean lunarMultiDraw() {
		return Features.LUNAR_MULTIDRAW.on;
	}

	public boolean immediateRing() {
		return Features.IMMEDIATE_RING.on;
	}

	public boolean movingBlockFlatLight() {
		return Features.MOVING_BLOCK_FLAT_LIGHT.on;
	}

	public boolean movingBlockCache() {
		return Features.MOVING_BLOCK_CACHE.on;
	}

	public boolean lookupCaches() {
		return Features.LOOKUP_CACHES.on;
	}

	public boolean microOpts() {
		return Features.MICRO_OPTS.on;
	}

	public boolean microOpts2() {
		return Features.MICRO_OPTS.stableResults;
	}

	public boolean microOpts3() {
		return Features.MICRO_OPTS.fewerChecks;
	}

	public boolean fastCubes() {
		return Features.MICRO_OPTS.fastCubes;
	}

	public boolean spawnerMesh() {
		return Features.MICRO_OPTS.spawnerMobMesh;
	}

	public boolean tboCache() {
		return Features.MICRO_OPTS.texBufferOnce && values.textureBuffer;
	}

	public boolean itemBounds() {
		return Features.MICRO_OPTS.sharedItemBounds;
	}

	public boolean hudCache() {
		return Features.HUD_CACHE.on;
	}

	public boolean skinAtlas() {
		return Features.SKIN_ATLAS.on;
	}

	public boolean writerCache() {
		return Features.WRITER_CACHE.on;
	}

	public boolean signCache() {
		return Features.SIGN_CACHE.on;
	}

	public boolean fboShare() {
		return Features.FBO_SHARE.on;
	}

	public boolean outlineSkip() {
		return Features.OUTLINE_SKIP.on;
	}

	public boolean ringZeroCopy() {
		return Features.RING_ZERO_COPY.on;
	}

	public boolean spawnerTickCache() {
		return Features.SPAWNER_TICK_CACHE.on;
	}

	public boolean spawnerReplay() {
		return Features.SPAWNER_REPLAY.on;
	}
}
