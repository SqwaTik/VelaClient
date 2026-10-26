package dev.sqwat.vela.mixin;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
abstract class PearlMouseMixin {
    @Inject(method="onButton",at=@At("HEAD"))
    private void vela$press(long window,MouseButtonInfo event,int action,CallbackInfo ci){
        if(action==org.lwjgl.glfw.GLFW.GLFW_PRESS)dev.sqwat.vela.module.ClickPearl.pressed(dev.sqwat.vela.ui.BindInput.mouse(event.button()));
    }
}
