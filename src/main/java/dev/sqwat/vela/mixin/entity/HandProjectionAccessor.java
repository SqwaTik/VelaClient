package dev.sqwat.vela.mixin.entity;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Projection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface HandProjectionAccessor {
    @Accessor("hudProjection") Projection vela$handProjection();
}
