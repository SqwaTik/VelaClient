package dev.sqwat.vela.mixin.screen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.module.visual.Wasted;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** Lets Wasted hold the death screen back until its effect ends. */
@Mixin(ClientPacketListener.class)
abstract class WastedDeathScreenMixin {
    @WrapOperation(method="handlePlayerCombatKill",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void vela$wasted(Gui gui,Screen screen,Operation<Void> original){
        if(!Wasted.intercept(screen))original.call(gui,screen);
    }
}
