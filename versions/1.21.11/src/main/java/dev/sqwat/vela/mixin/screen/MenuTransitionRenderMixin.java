package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.ui.MenuTransition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** 1.21.11: GameRenderer draws the screen (26.x: Gui.extractRenderState). Scales it in with the menu transition. */
@Mixin(GameRenderer.class)
abstract class MenuTransitionRenderMixin {
    @Redirect(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void vela$render(Screen screen,GuiGraphics g,int x,int y,float delta){
        float scale=MenuTransition.frame(screen);g.pose().pushMatrix();
        g.pose().translate(g.guiWidth()/2f,g.guiHeight()/2f);g.pose().scale(scale);g.pose().translate(-g.guiWidth()/2f,-g.guiHeight()/2f);
        try{screen.renderWithTooltipAndSubtitles(g,(int)Math.round(MenuTransition.x(x)),(int)Math.round(MenuTransition.y(y)),delta);}finally{g.pose().popMatrix();}
        dev.sqwat.vela.marker.MarkerNotices.draw(g);
    }
}
