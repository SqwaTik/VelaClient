package dev.sqwat.vela.mixin.entity;

import dev.sqwat.vela.module.utility.RenderTweaks;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keeps the painting geometry but replaces its front artwork with the existing canvas backing. */
@Mixin(PaintingRenderer.class)
abstract class PaintingArtworkMixin {
    @ModifyArg(method="submit",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/texture/TextureAtlas;getSprite(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;",ordinal=0),index=0)
    private Identifier vela$canvas(Identifier original){return RenderTweaks.hide("pictures")?Identifier.withDefaultNamespace("back"):original;}
}
