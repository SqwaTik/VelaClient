package dev.sqwat.vela.mixin.world;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.sqwat.vela.render.shader.SkyShaderRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.3: the whole sky is one render pass, so the shader sky replaces all of it (disc, end sky and celestials). */
@Mixin(SkyRenderer.class)
abstract class ProceduralSkyMixin {
    @Shadow @Final private RenderTarget renderTarget;
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void vela$sky(GpuBufferSlice fog,SkyRenderState state,CallbackInfo ci){
        if(!SkyShaderRenderer.render(renderTarget))return;
        RenderSystem.setShaderFog(fog);
        ci.cancel();
    }
    @Inject(method="close",at=@At("HEAD"))
    private void vela$close(CallbackInfo ci){SkyShaderRenderer.close();}
}
