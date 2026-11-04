package dev.sqwat.vela.mixin.compat;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A composite render type's state (1.21.5: the textures sit in its CompositeState). */
@Mixin(targets="net.minecraft.client.renderer.RenderType$CompositeRenderType")
public interface RenderTypeAccessor {
    @Accessor("state") RenderType.CompositeState vela$state();
}
