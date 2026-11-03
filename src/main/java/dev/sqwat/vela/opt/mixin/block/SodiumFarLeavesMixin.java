package dev.sqwat.vela.opt.mixin.block;

import com.llamalad7.mixinextras.sugar.Local;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.sqwat.vela.opt.block.FarLeaves;

// the layer of each quad is resolved in processQuad; one renderer per builder thread. The model's class moved in 26.2,
// so the block comes in by type
// priority above the default: BetterBlockEntities overwrites processQuad at 1000, and Mixin lets only a later mixin
// inject into a method another one merged
@Mixin(value = BlockRenderer.class, remap = false, priority = 1100)
public class SodiumFarLeavesMixin {
	@Unique
	private BlockState velaopt$state;
	@Unique
	private BlockPos velaopt$pos;
	@Unique
	private final FarLeaves.Section velaopt$section = new FarLeaves.Section();

	@Inject(require = 0, method = "renderModel", at = @At("HEAD"), cancellable = true)
	private void velaopt$block(CallbackInfo ci, @Local(argsOnly = true) BlockState state, @Local(argsOnly = true, ordinal = 0) BlockPos pos) {
		velaopt$state = state;
		velaopt$pos = pos;
		if (FarLeaves.hidePlant(velaopt$section, state, pos)) {
			ci.cancel();
		}
	}

	// on every quad of every block: only the layer argument is swapped, no argument array or wrapper object
	@ModifyArg(require = 0, method = "processQuad", at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forChunkLayer(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;)Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;"))
	private ChunkSectionLayer velaopt$farLeaves(ChunkSectionLayer layer) {
		return FarLeaves.type(velaopt$section, velaopt$state, velaopt$pos, layer);
	}
}
