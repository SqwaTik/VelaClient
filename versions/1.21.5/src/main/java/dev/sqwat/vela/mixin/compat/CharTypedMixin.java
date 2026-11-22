package dev.sqwat.vela.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.compat.input.CharacterEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Asks {@link dev.sqwat.vela.compat.CharEvents} before a typed character reaches the screen; 1.21.5 passes (char, modifiers). */
@Mixin(KeyboardHandler.class)
abstract class CharTypedMixin {
    @WrapOperation(method="charTyped",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;charTyped(CI)Z"))
    private boolean vela$allowCharType(Screen screen,char character,int modifiers,Operation<Boolean> original){
        return dev.sqwat.vela.compat.CharEvents.allow(screen,new CharacterEvent(character,modifiers))&&original.call(screen,character,modifiers);
    }
}
