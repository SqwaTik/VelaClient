package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.ui.MenuTransition;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(MouseHandler.class)
public abstract class MenuPointerMixin {
    @Redirect(method="onButton",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z"))
    private boolean vela$click(Screen screen,MouseButtonEvent e,boolean doubled){return dev.sqwat.vela.marker.MarkerNotices.click(e.x(),e.y(),e.button())||screen.mouseClicked(MenuTransition.mouse(e),doubled);}
    @Redirect(method="onButton",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z"))
    private boolean vela$release(Screen screen,MouseButtonEvent e){return screen.mouseReleased(MenuTransition.mouse(e));}
    @Redirect(method="handleAccumulatedMovement",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V"))
    private void vela$move(Screen screen,double x,double y){screen.mouseMoved(MenuTransition.x(x),MenuTransition.y(y));}
    @Redirect(method="handleAccumulatedMovement",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"))
    private boolean vela$drag(Screen screen,MouseButtonEvent e,double x,double y){return screen.mouseDragged(MenuTransition.mouse(e),MenuTransition.delta(x),MenuTransition.delta(y));}
    @Redirect(method="onScroll",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean vela$scroll(Screen screen,double x,double y,double h,double v){return screen.mouseScrolled(MenuTransition.x(x),MenuTransition.y(y),h,v);}
}
