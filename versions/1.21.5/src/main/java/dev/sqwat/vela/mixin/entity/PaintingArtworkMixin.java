package dev.sqwat.vela.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.sqwat.vela.module.utility.RenderTweaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps the painting geometry but replaces its front artwork with the existing canvas backing; 1.21.5 looks the sprite up in render. */
@Mixin(PaintingRenderer.class)
abstract class PaintingArtworkMixin {
    @ModifyExpressionValue(method="render(Lnet/minecraft/client/renderer/entity/state/PaintingRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/resources/PaintingTextureManager;get(Lnet/minecraft/world/entity/decoration/PaintingVariant;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"))
    private TextureAtlasSprite vela$canvas(TextureAtlasSprite original){return RenderTweaks.hide("pictures")?Minecraft.getInstance().getPaintingTextures().getBackSprite():original;}
}
