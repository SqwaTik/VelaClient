package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.ui.MenuTransition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
public abstract class MenuTransitionMixin {
    @Inject(method="setScreen",at=@At("RETURN"))
    private void vela$screen(Screen screen,CallbackInfo ci){
        MenuTransition.start(Minecraft.getInstance().gui.screen());
        if(screen!=null){
            var options=Minecraft.getInstance().options;
            options.keyUp.setDown(false);options.keyDown.setDown(false);options.keyLeft.setDown(false);options.keyRight.setDown(false);
            options.keyJump.setDown(false);options.keySprint.setDown(false);
        }
    }
    @Redirect(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void vela$render(Screen screen,GuiGraphicsExtractor g,int x,int y,float delta){
        float scale=MenuTransition.frame(screen);g.pose().pushMatrix();
        g.pose().translate(g.guiWidth()/2f,g.guiHeight()/2f);g.pose().scale(scale);g.pose().translate(-g.guiWidth()/2f,-g.guiHeight()/2f);
        try{screen.extractRenderStateWithTooltipAndSubtitles(g,(int)Math.round(MenuTransition.x(x)),(int)Math.round(MenuTransition.y(y)),delta);}finally{g.pose().popMatrix();}
        dev.sqwat.vela.marker.MarkerNotices.draw(g);
    }
}
