package dev.sqwat.vela.mixin.input;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardHandler.class)
abstract class PearlKeyboardMixin {
    @Inject(method="keyPress",at=@At("HEAD"))
    private void vela$press(long window,int action,KeyEvent event,CallbackInfo ci){
        if(action==org.lwjgl.glfw.GLFW.GLFW_PRESS)dev.sqwat.vela.module.utility.ClickPearl.pressed(event.key());
    }
}
