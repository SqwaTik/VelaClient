package dev.sqwat.vela.mixin.hud;
import dev.sqwat.vela.module.visual.ReplayVideo;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** End of a fully drawn frame: the place to capture a Death Recap video frame. */
@Mixin(GameRenderer.class)
abstract class ReplayVideoFrameMixin {
    @Inject(method="render",at=@At("TAIL"))
    private void vela$captureReplayFrame(CallbackInfo ci){if(ReplayVideo.running())ReplayVideo.frameEnd();}
}
