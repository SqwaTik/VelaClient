package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.module.visual.Wasted;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
/** While Wasted presents the death: its overlay replaces the red background and vanilla title; buttons fade in. */
@Mixin(DeathScreen.class)
abstract class WastedDeathScreenStyleMixin {
    @Shadow @Final private Component causeOfDeath;
    @Shadow @Final private List<Button> exitButtons;
    @Inject(method="renderBackground",at=@At("HEAD"),cancellable=true)
    private void vela$background(GuiGraphics g,int mouseX,int mouseY,float partial,CallbackInfo ci){
        if(Wasted.drawScreen(g,causeOfDeath))ci.cancel();
    }
    @Redirect(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private void vela$title(GuiGraphics g,Font font,Component text,int x,int y,int color){if(!Wasted.presenting())g.drawCenteredString(font,text,x,y,color);}
    @Inject(method="render",at=@At("HEAD"))
    private void vela$fadeButtons(GuiGraphics g,int mouseX,int mouseY,float partial,CallbackInfo ci){
        float alpha=Wasted.buttonAlpha();
        if(!Wasted.presenting()){for(var button:exitButtons)button.setAlpha(alpha);return;}
        int y=Wasted.buttonTop(((net.minecraft.client.gui.screens.Screen)(Object)this).height);
        for(var button:exitButtons){button.setAlpha(alpha);button.setY(y);y+=24;}
    }
}
