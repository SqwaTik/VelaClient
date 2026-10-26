package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.module.visual.Wasted;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Gui.setScreen(null) while dead builds a fresh DeathScreen, so any screen change during the Wasted effect (closing
 * chat, another mod) showed the buttons at once. While the effect runs, such a death screen is taken into the hold
 * instead; Wasted opens it itself when the effect ends.
 */
@Mixin(Gui.class)
abstract class WastedHoldScreenMixin {
    @Shadow private Screen screen;
    @Shadow @Final private Minecraft minecraft;
    @Inject(method="setScreen",at=@At("TAIL"))
    private void vela$holdDeathScreen(Screen requested,CallbackInfo ci){
        if(screen instanceof DeathScreen death&&Wasted.hold(death)){
            death.removed();
            screen=null;
            minecraft.mouseHandler.grabMouse();
        }
    }
}
