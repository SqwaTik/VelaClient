package dev.sqwat.vela.mixin.screen;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The "Ритм" profile also replaces the click of vanilla buttons, so every press in every menu sounds the same. */
@Mixin(AbstractWidget.class)
abstract class RhythmClickMixin {
    @Inject(method="playButtonClickSound",at=@At("HEAD"),cancellable=true)
    private static void vela$rhythm(SoundManager soundManager,CallbackInfo ci){
        if(dev.sqwat.vela.ui.UiSounds.replaceVanillaClick())ci.cancel();
    }
}
