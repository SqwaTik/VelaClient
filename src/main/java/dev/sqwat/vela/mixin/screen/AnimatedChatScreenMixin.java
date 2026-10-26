package dev.sqwat.vela.mixin.screen;

import dev.sqwat.vela.module.visual.Animations;
import dev.sqwat.vela.ui.animation.FiniteMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin(ChatScreen.class)
abstract class AnimatedChatScreenMixin {
    @Shadow protected EditBox input;
    @Unique private long vela$opened;
    @Unique private int vela$baseY,vela$offset;

    @Inject(method="init",at=@At("RETURN"))
    private void vela$init(CallbackInfo ci){vela$opened=0;vela$baseY=input.getY();vela$offset=0;}

    @Inject(method="extractRenderState",at=@At("HEAD"))
    private void vela$frame(GuiGraphicsExtractor g,int x,int y,float delta,CallbackInfo ci){
        long now=System.nanoTime();if(vela$opened==0)vela$opened=now;
        var mc=Minecraft.getInstance();
        float remaining=1-Math.min(1,(now-vela$opened)/170_000_000f);
        vela$offset=Animations.active("chat")&&mc.player!=null&&!mc.player.isSleeping()
            ?Math.round(FiniteMotion.back(remaining)*8*mc.getWindow().getHeight()/1080f):0;
        // Moving the widget itself keeps native selection and mouse hit testing aligned.
        input.setY(vela$baseY+vela$offset);
    }

    @WrapOperation(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private void vela$background(GuiGraphicsExtractor g,int x0,int y0,int x1,int y1,int color,Operation<Void> original){
        original.call(g,x0,y0+vela$offset,x1,y1+vela$offset,color);
    }
}
