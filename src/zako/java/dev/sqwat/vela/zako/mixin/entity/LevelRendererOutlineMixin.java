// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.entity;

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
import dev.sqwat.vela.zako.ZakoOptConfig;

@Mixin(LevelRenderer.class)
public class LevelRendererOutlineMixin {
	@Unique
	private boolean zakoopt$glowing = true;
	@Unique
	private boolean zakoopt$outlineDirty = true;
	@Unique private GpuTexture zakoopt$lastColor;
	@Unique private GpuTexture zakoopt$lastDepth;

	// true when something was submitted with an outline this frame
	@ModifyExpressionValue(method = "addMainPass", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;hasAnyOutline()Z"))
	private boolean zakoopt$glowing(boolean any) {
		zakoopt$glowing = any;
		return any;
	}

	// vanilla clears the outline target every frame; without glowing entities it is already clear from the last frame
	@WrapOperation(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;Lorg/joml/Vector4fc;Lcom/mojang/blaze3d/textures/GpuTexture;D)V"))
	private void zakoopt$clearOutline(CommandEncoder encoder, GpuTexture color, Vector4fc clearColor, GpuTexture depth, double clearDepth, Operation<Void> original) {
		if (color != zakoopt$lastColor || depth != zakoopt$lastDepth || zakoopt$glowing || zakoopt$outlineDirty || !ZakoOptConfig.outlineSkip()) {
			original.call(encoder, color, clearColor, depth, clearDepth);
		}
		zakoopt$lastColor = color;
		zakoopt$lastDepth = depth;
		zakoopt$outlineDirty = zakoopt$glowing;
	}

	// ...and blends that empty target over the frame
	@Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
	private void zakoopt$skipEmptyOutline(CallbackInfo ci) {
		if (!zakoopt$glowing && ZakoOptConfig.outlineSkip()) {
			ci.cancel();
		}
	}
}
