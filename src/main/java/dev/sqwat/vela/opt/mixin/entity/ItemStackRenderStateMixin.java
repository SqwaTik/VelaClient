package dev.sqwat.vela.opt.mixin.entity;

import dev.sqwat.vela.opt.feature.Features;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import dev.sqwat.vela.opt.entity.ItemBounds;

@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin {
	@Shadow
	private int activeLayerCount;
	@Shadow
	private ItemStackRenderState.LayerRenderState[] layers;

	@WrapMethod(method = "getModelBoundingBox")
	private AABB velaopt$sharedBounds(Operation<AABB> original) {
		if (!Features.MICRO_OPTS.sharedItemBounds || activeLayerCount != 1) {
			return original.call();
		}
		ItemLayerAccessor layer = (ItemLayerAccessor) layers[0];
		if ((layer.velaopt$localTransform().properties() & Matrix4fc.PROPERTY_IDENTITY) == 0) {
			return original.call();
		}
		return ItemBounds.get(layer.velaopt$extents(), layer.velaopt$itemTransform(), original::call);
	}
}
