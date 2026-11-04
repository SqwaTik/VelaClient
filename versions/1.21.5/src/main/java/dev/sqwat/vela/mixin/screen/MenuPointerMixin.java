package dev.sqwat.vela.mixin.screen;

import dev.sqwat.vela.ui.MenuTransition;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** 1.21.5: the screen's mouse methods take coordinates and the button instead of an event. */
@Mixin(MouseHandler.class)
public abstract class MenuPointerMixin {
    @Redirect(method="onPress",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseClicked(DDI)Z"))
    private boolean vela$click(Screen screen,double x,double y,int button){return dev.sqwat.vela.marker.MarkerNotices.click(x,y,button)||screen.mouseClicked(MenuTransition.x(x),MenuTransition.y(y),button);}
    @Redirect(method="onPress",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseReleased(DDI)Z"))
    private boolean vela$release(Screen screen,double x,double y,int button){return screen.mouseReleased(MenuTransition.x(x),MenuTransition.y(y),button);}
    @Redirect(method="handleAccumulatedMovement",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V"))
    private void vela$move(Screen screen,double x,double y){screen.mouseMoved(MenuTransition.x(x),MenuTransition.y(y));}
    @Redirect(method="handleAccumulatedMovement",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseDragged(DDIDD)Z"))
    private boolean vela$drag(Screen screen,double x,double y,int button,double dx,double dy){return screen.mouseDragged(MenuTransition.x(x),MenuTransition.y(y),button,MenuTransition.delta(dx),MenuTransition.delta(dy));}
    @Redirect(method="onScroll",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z"))
    private boolean vela$scroll(Screen screen,double x,double y,double h,double v){return screen.mouseScrolled(MenuTransition.x(x),MenuTransition.y(y),h,v);}
}
