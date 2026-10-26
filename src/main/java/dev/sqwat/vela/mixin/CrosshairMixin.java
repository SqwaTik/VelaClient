package dev.sqwat.vela.mixin;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.sqwat.vela.module.Crosshair;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Hud.class)
abstract class CrosshairMixin {
    @Redirect(method="extractCrosshair",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",ordinal=0))
    private void vela$crosshair(GuiGraphicsExtractor g,RenderPipeline pipeline,Identifier sprite,int x,int y,int w,int h){
        var custom=Crosshair.current();if(custom==null)g.blitSprite(pipeline,sprite,x,y,w,h);else custom.draw(g);
    }
}
