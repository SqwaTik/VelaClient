package dev.sqwat.vela.mixin.world;
import dev.sqwat.vela.render.shader.SkyShaderRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 1.21.11: the sky is drawn straight to the main render target. */
@Mixin(SkyRenderer.class)
abstract class ProceduralSkyMixin {
    @Inject(method="renderSkyDisc",at=@At("HEAD"),cancellable=true)
    private void vela$sky(int color,CallbackInfo ci){if(SkyShaderRenderer.render(Minecraft.getInstance().getMainRenderTarget()))ci.cancel();}
    @Inject(method="renderEndSky",at=@At("HEAD"),cancellable=true)
    private void vela$endSky(CallbackInfo ci){if(SkyShaderRenderer.render(Minecraft.getInstance().getMainRenderTarget()))ci.cancel();}
    @Inject(method={"renderDarkDisc","renderSunMoonAndStars","renderSunriseAndSunset","renderEndFlash"},at=@At("HEAD"),cancellable=true)
    private void vela$celestials(CallbackInfo ci){if(SkyShaderRenderer.rendered())ci.cancel();}
    @Inject(method="close",at=@At("HEAD"))
    private void vela$close(CallbackInfo ci){SkyShaderRenderer.close();}
}
