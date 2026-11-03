package dev.sqwat.vela.opt.mixin.entity;

import dev.sqwat.vela.opt.feature.Features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Vector4fc;

@Mixin(LevelRenderer.class)
public class LevelRendererOutlineMixin {
	@Unique
	private boolean velaopt$glowing = true;
	@Unique
	private boolean velaopt$outlineDirty = true;

	// true when something was submitted with an outline this frame
	@ModifyExpressionValue(method = "addMainPass*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;hasAnyOutline()Z"))
	private boolean velaopt$glowing(boolean any) {
		velaopt$glowing = any;
		return any;
	}

	// vanilla clears the outline target every frame; without glowing entities it is already clear from the last frame
	@WrapOperation(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/blaze3d/textures/GpuTexture;D)V"))
	private void velaopt$clearOutline(CommandEncoder encoder, GpuTexture color, Vector4fc clearColor, GpuTexture depth, double clearDepth, Operation<Void> original) {
		if (velaopt$glowing || velaopt$outlineDirty || !Features.OUTLINE_SKIP.on) {
			original.call(encoder, color, clearColor, depth, clearDepth);
		}
		velaopt$outlineDirty = velaopt$glowing;
	}

	// ...and blends that empty target over the frame
	@Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
	private void velaopt$skipEmptyOutline(CallbackInfo ci) {
		if (!velaopt$glowing && Features.OUTLINE_SKIP.on) {
			ci.cancel();
		}
	}
}
