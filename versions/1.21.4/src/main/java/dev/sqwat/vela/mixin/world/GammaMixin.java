package dev.sqwat.vela.mixin.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.shaders.AbstractUniform;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 1.21.4: LightTexture.updateLightTexture looks the lightmap uniforms up by name; night vision and brightness get a boosting view. */
@Mixin(LightTexture.class)
abstract class GammaMixin {
    @WrapOperation(method="updateLightTexture",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/CompiledShaderProgram;safeGetUniform(Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/AbstractUniform;"))
    private AbstractUniform vela$lightmap(CompiledShaderProgram shader,String name,Operation<AbstractUniform> original){
        var uniform=original.call(shader,name);
        return "NightVisionFactor".equals(name)||"BrightnessFactor".equals(name)?new dev.sqwat.vela.compat.BoostedUniform(uniform):uniform;
    }
}
