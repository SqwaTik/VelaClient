package dev.sqwat.vela.mixin.perf;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.render.perf.HudCache;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** HUD cache: the HUD, toasts, the debug overlay and subtitles are extracted on refresh frames only. */
@Mixin(Gui.class)
abstract class HudCacheGuiMixin {
    @Shadow @Final private Minecraft minecraft;
    @Inject(method="extractRenderState",at=@At("HEAD"))
    private void vela$hudFrame(DeltaTracker delta,boolean shouldRenderLevel,boolean resourcesLoaded,CallbackInfo ci){HudCache.beginFrame(minecraft,shouldRenderLevel);}
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void vela$cachedHud(Hud hud,GuiGraphicsExtractor g,DeltaTracker delta,Operation<Void> original){
        if(HudCache.reuse()){
            // Normally set by the skipped extraction; the hand and the 3D crosshair read it.
            minecraft.gameRenderer.gameRenderState().guiRenderState.isHudHidden=hud.isHidden();
            return;
        }
        if(!HudCache.refresh()){original.call(hud,g,delta);return;}
        HudCache.extracting(true);
        try{original.call(hud,g,delta);}finally{HudCache.extracting(false);}
    }
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Hud;extractSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void vela$cachedSaving(Hud hud,GuiGraphicsExtractor g,DeltaTracker delta,Operation<Void> original){if(!HudCache.reuse())original.call(hud,g,delta);}
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/toasts/ToastManager;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void vela$cachedToasts(ToastManager toasts,GuiGraphicsExtractor g,Operation<Void> original){if(!HudCache.reuse())original.call(toasts,g);}
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Hud;extractDebugOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void vela$cachedDebug(Hud hud,GuiGraphicsExtractor g,Operation<Void> original){if(!HudCache.reuse())original.call(hud,g);}
    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Hud;extractDeferredSubtitles()V"))
    private void vela$cachedSubtitles(Hud hud,Operation<Void> original){if(!HudCache.reuse())original.call(hud);}
}
