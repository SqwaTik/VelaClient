package dev.sqwat.vela.mixin.render;

import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.frontend.shaders.GlslCompiler;
import dev.sqwat.vela.compat.SpirvGlsl;
import net.minecraft.client.renderer.ShaderDefines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 26.3: Vela's GLSL 330 shaders (files and the custom sky) get the SPIR-V locations right before compiling. */
@Mixin(GlslCompiler.class)
abstract class SpirvGlslMixin {
    @ModifyVariable(method = "compileToSpv", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private String vela$spirv(String source, String name, String sourceAgain, ShaderType type, ShaderDefines defines, ShaderSource includes) {
        return SpirvGlsl.ours(name) ? SpirvGlsl.port(source, type == ShaderType.VERTEX) : source;
    }
}
