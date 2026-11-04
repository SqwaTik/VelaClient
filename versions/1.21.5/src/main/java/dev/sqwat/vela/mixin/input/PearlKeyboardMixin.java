package dev.sqwat.vela.mixin.input;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.5: keyPress takes the raw GLFW key, scancode, action and modifiers. */
@Mixin(KeyboardHandler.class)
abstract class PearlKeyboardMixin {
    @Inject(method="keyPress",at=@At("HEAD"))
    private void vela$press(long window,int key,int scancode,int action,int modifiers,CallbackInfo ci){
        if(action==org.lwjgl.glfw.GLFW.GLFW_PRESS)dev.sqwat.vela.module.utility.ClickPearl.pressed(key);
    }
}
