package dev.sqwat.vela.mixin.hud;

import dev.sqwat.vela.ui.screen.VelaScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Movement tutorial cards must not cover the settings/search controls of a custom screen. */
@Mixin(TutorialToast.class)
abstract class TutorialToastMixin {
    @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
    private void vela$keepControlsAccessible(GuiGraphicsExtractor graphics,Font font,long visibleFor,CallbackInfo ci){
        if(Minecraft.getInstance().gui.screen() instanceof VelaScreen)ci.cancel();
    }
}
