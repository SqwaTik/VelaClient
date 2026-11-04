package dev.sqwat.vela.mixin.hud;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 1.21.5 has no GuiRenderer: the scene is captured once the world is drawn, when GameRenderer starts the GUI pass. */
@Mixin(GameRenderer.class)
public abstract class GlassCaptureMixin {
    @Inject(method="render",at=@At(value="NEW",target="net/minecraft/client/gui/GuiGraphics"))
    private void vela$captureScene(CallbackInfo ci){dev.sqwat.vela.ui.render.GlassSurface.capture();dev.sqwat.vela.render.shader.TemporalBlur.capture();}
}
