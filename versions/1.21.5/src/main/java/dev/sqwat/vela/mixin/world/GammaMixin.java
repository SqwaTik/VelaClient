package dev.sqwat.vela.mixin.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.21.5: LightTexture.updateLightTexture sets the lightmap uniforms by name, so night vision and brightness are picked by name. */
@Mixin(LightTexture.class)
abstract class GammaMixin {
    @WrapOperation(method="updateLightTexture",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/systems/RenderPass;setUniform(Ljava/lang/String;[F)V"))
    private void vela$lightmap(RenderPass pass,String name,float[] values,Operation<Void> original){
        if(values.length==1&&("NightVisionFactor".equals(name)||"BrightnessFactor".equals(name)))values=new float[]{dev.sqwat.vela.module.utility.Gamma.boost(values[0])};
        original.call(pass,name,values);
    }
}
