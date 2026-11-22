package dev.sqwat.vela.opt.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.sqwat.vela.opt.feature.Features;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1.21.11: the render state knows whether anything glows; the main pass lambda (method_62214) clears the outline target
@Mixin(LevelRenderer.class)
public class LevelRendererOutlineMixin {
	@Shadow
	@Final
	private LevelRenderState levelRenderState;
	@Unique
	private boolean velaopt$outlineDirty = true;

	// vanilla clears the outline target every frame; without glowing entities it is already clear from the last frame
	@WrapOperation(method = "method_62214", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V"))
	private void velaopt$clearOutline(CommandEncoder encoder, GpuTexture color, int clearColor, GpuTexture depth, double clearDepth, Operation<Void> original) {
		boolean glowing = levelRenderState.haveGlowingEntities;
		if (glowing || velaopt$outlineDirty || !Features.OUTLINE_SKIP.on) {
			original.call(encoder, color, clearColor, depth, clearDepth);
		}
		velaopt$outlineDirty = glowing;
	}

	// ...and blends that empty target over the frame
	@Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
	private void velaopt$skipEmptyOutline(CallbackInfo ci) {
		if (!levelRenderState.haveGlowingEntities && Features.OUTLINE_SKIP.on) {
			ci.cancel();
		}
	}
}
