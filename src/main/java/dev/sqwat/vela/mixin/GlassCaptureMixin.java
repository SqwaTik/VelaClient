package dev.sqwat.vela.mixin;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiRenderer.class)
public abstract class GlassCaptureMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void vela$captureScene(CallbackInfo ci){if(dev.sqwat.vela.zako.hud.HudCache.additionalPass)return;dev.sqwat.vela.ui.render.GlassSurface.capture();dev.sqwat.vela.render.TemporalBlur.capture();}
}
