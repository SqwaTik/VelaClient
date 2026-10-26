// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.entity;

import dev.sqwat.vela.zako.feature.Features;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.sqwat.vela.zako.entity.ItemBounds;

@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin {
	@Shadow
	private int activeLayerCount;
	@Shadow
	private ItemStackRenderState.LayerRenderState[] layers;

	@WrapMethod(method = "getModelBoundingBox")
	private AABB zakoopt$sharedBounds(Operation<AABB> original) {
		if (!Features.MICRO_OPTS.sharedItemBounds || activeLayerCount != 1) {
			return original.call();
		}
		ItemLayerAccessor layer = (ItemLayerAccessor) layers[0];
		if ((layer.zakoopt$localTransform().properties() & Matrix4fc.PROPERTY_IDENTITY) == 0) {
			return original.call();
		}
		return ItemBounds.get(layer.zakoopt$extents(), layer.zakoopt$itemTransform(), original::call);
	}
}
