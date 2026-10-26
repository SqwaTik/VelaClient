package dev.sqwat.vela.mixin;
import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.sqwat.vela.render.SkyShaderRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(SkyRenderer.class)
abstract class ProceduralSkyMixin {
    @Shadow @Final private RenderTarget renderTarget;
    @Inject(method="renderSkyDisc",at=@At("HEAD"),cancellable=true)
    private void vela$sky(int color,CallbackInfo ci){if(SkyShaderRenderer.render(renderTarget))ci.cancel();}
    @Inject(method="renderEndSky",at=@At("HEAD"),cancellable=true)
    private void vela$endSky(CallbackInfo ci){if(SkyShaderRenderer.render(renderTarget))ci.cancel();}
    @Inject(method={"renderDarkDisc","renderSunMoonAndStars","renderSunriseAndSunset","renderEndFlash"},at=@At("HEAD"),cancellable=true)
    private void vela$celestials(CallbackInfo ci){if(SkyShaderRenderer.rendered())ci.cancel();}
    @Inject(method="close",at=@At("HEAD"))
    private void vela$close(CallbackInfo ci){SkyShaderRenderer.close();}
}
