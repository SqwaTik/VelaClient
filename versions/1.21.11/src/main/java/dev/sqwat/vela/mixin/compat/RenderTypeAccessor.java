package dev.sqwat.vela.mixin.compat;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A render type's textures (26.x: RenderType.prepare().textures()). */
@Mixin(RenderType.class)
public interface RenderTypeAccessor {
    @Accessor("state") RenderSetup vela$state();
}
