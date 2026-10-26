// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import dev.sqwat.vela.zako.ZakoOptConfig;
import dev.sqwat.vela.zako.block.BlockEntityDistance;

// Reuse the renderer already selected by vanilla; null follows its existing early-return path.
@Mixin(value = BlockEntityRenderDispatcher.class, priority = 500)
public class BlockEntityRenderDispatcherCullMixin {
	@Shadow
	private Vec3 cameraPos;

	// vanilla returns null for these further down: every check there only returns null, so checking distance first changes nothing
	@SuppressWarnings({"unchecked", "rawtypes"})
	@ModifyExpressionValue(method = "tryExtractRenderState", at = @At(value="INVOKE", target="Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"))
	private BlockEntityRenderer zakoopt$earlyCull(BlockEntityRenderer renderer, BlockEntity blockEntity) {
		return renderer != null && ZakoOptConfig.blockEntityEarlyCull() && cameraPos != null
				&& !BlockEntityDistance.inDistance(renderer, blockEntity, cameraPos) ? null : renderer;
	}
}
