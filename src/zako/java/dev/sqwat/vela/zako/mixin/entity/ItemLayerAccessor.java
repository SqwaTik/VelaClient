// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.mixin.entity;

import java.util.function.Supplier;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemLayerAccessor {
	@Accessor("extents")
	Supplier<Vector3fc[]> zakoopt$extents();

	@Accessor("itemTransform")
	ItemTransform zakoopt$itemTransform();

	@Accessor("localTransform")
	Matrix4f zakoopt$localTransform();
}
