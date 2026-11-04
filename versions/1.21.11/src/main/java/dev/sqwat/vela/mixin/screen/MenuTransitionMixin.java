package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.ui.MenuTransition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** 1.21.11: Minecraft holds the screen (26.x moved it to Gui); the scaled screen draw is a GameRenderer mixin. */
@Mixin(Minecraft.class)
public abstract class MenuTransitionMixin {
    @Inject(method="setScreen",at=@At("RETURN"))
    private void vela$screen(Screen screen,CallbackInfo ci){
        MenuTransition.start(Minecraft.getInstance().screen);
        if(screen!=null){
            var options=Minecraft.getInstance().options;
            options.keyUp.setDown(false);options.keyDown.setDown(false);options.keyLeft.setDown(false);options.keyRight.setDown(false);
            options.keyJump.setDown(false);options.keySprint.setDown(false);
        }
    }
}
