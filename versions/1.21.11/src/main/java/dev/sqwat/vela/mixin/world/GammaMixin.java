package dev.sqwat.vela.mixin.world;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
/** LightTexture.updateLightTexture writes the lightmap uniforms in order: ambient, sky, block, night vision (3), darkness, darken world, brightness (6). */
@Mixin(LightTexture.class)
abstract class GammaMixin {
    @ModifyArg(method="updateLightTexture",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",ordinal=3))
    private float vela$nightVision(float value){return dev.sqwat.vela.module.utility.Gamma.boost(value);}
    @ModifyArg(method="updateLightTexture",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/buffers/Std140Builder;putFloat(F)Lcom/mojang/blaze3d/buffers/Std140Builder;",ordinal=6))
    private float vela$brightness(float value){return dev.sqwat.vela.module.utility.Gamma.boost(value);}
}
