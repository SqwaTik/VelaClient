package dev.sqwat.vela.mixin.hud;
import dev.sqwat.vela.module.visual.Crosshair;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import java.util.function.Function;
@Mixin(Gui.class)
abstract class CrosshairMixin {
    @Redirect(method="renderCrosshair",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V",ordinal=0))
    private void vela$crosshair(GuiGraphics g,Function<ResourceLocation,RenderType> type,ResourceLocation sprite,int x,int y,int w,int h){
        var custom=Crosshair.current();if(custom==null)g.blitSprite(type,sprite,x,y,w,h);else custom.draw(g);
    }
}
