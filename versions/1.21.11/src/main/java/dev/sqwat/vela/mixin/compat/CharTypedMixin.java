package dev.sqwat.vela.mixin.compat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Asks {@link dev.sqwat.vela.compat.CharEvents} before a typed character reaches the screen. */
@Mixin(KeyboardHandler.class)
abstract class CharTypedMixin {
    @WrapOperation(method="charTyped",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z"))
    private boolean vela$allowCharType(Screen screen,CharacterEvent event,Operation<Boolean> original){
        return dev.sqwat.vela.compat.CharEvents.allow(screen,event)&&original.call(screen,event);
    }
}
