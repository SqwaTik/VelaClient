// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.entity;

import dev.sqwat.vela.zako.feature.Features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemClusterRenderState.class)
public class ItemClusterRenderStateMixin {
	@ModifyExpressionValue(method = "extractItemGroupRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/state/ItemClusterRenderState;getRenderedAmount(I)I"))
	private int zakoopt$singleCopy(int copies) {
		return Features.ITEM_SINGLE_COPY.on ? 1 : copies;
	}
}
