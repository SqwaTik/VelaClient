package dev.sqwat.vela.mixin.screen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.module.visual.Wasted;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * While the Wasted effect runs, Gui.setScreen must not show a death screen: setScreen(null) while dead builds one
 * (closing chat, grabbing the mouse and other mods all call it), and an explicit DeathScreen is taken into the hold.
 * Done at the source: undoing it afterwards re-entered setScreen through MouseHandler.grabMouse and overflowed the stack.
 */
@Mixin(Gui.class)
abstract class WastedHoldScreenMixin {
    @Inject(method="setScreen",at=@At("HEAD"),cancellable=true)
    private void vela$holdExplicit(Screen requested,CallbackInfo ci){
        if(requested instanceof DeathScreen death&&Wasted.hold(death))ci.cancel();
    }
    @WrapOperation(method="setScreen",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;isDeadOrDying()Z"))
    private boolean vela$noImplicitDeathScreen(LocalPlayer player,Operation<Boolean> original){
        return original.call(player)&&!Wasted.effectRunning();
    }
}
