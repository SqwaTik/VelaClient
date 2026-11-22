package dev.sqwat.vela.mixin.input;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.5: MouseHandler.onPress takes the raw button, action and modifiers. */
@Mixin(MouseHandler.class)
abstract class PearlMouseMixin {
    @Inject(method="onPress",at=@At("HEAD"))
    private void vela$press(long window,int button,int action,int modifiers,CallbackInfo ci){
        if(action==org.lwjgl.glfw.GLFW.GLFW_PRESS)dev.sqwat.vela.module.utility.ClickPearl.pressed(dev.sqwat.vela.ui.input.BindInput.mouse(button));
    }
}
