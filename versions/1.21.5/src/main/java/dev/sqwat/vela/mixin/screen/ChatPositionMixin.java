package dev.sqwat.vela.mixin.screen;

import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.ui.render.Rect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.5 has no chat access layer: the pose is moved around render, and the screen-to-chat mapping that hover and
 * clicks use gets the inverse transform.
 */
@Mixin(ChatComponent.class)
public abstract class ChatPositionMixin {
    @Shadow protected abstract int getWidth();
    @Shadow protected abstract int getHeight();
    @Unique private float vela$x,vela$y,vela$scale=1,vela$originY;
    @Unique private boolean vela$moved;

    @Inject(method="render",at=@At("HEAD"))
    private void vela$chatPose(GuiGraphics g,int ticks,int mouseX,int mouseY,boolean focused,CallbackInfo ci){
        g.pose().pushPose();
        vela$moved=false;
        if(VelaClient.services()==null)return;
        var client=net.minecraft.client.Minecraft.getInstance();var layout=VelaClient.services().hudLayout();
        int screenHeight=g.guiHeight();
        layout.viewport(client.getWindow().calculateScale(client.options.guiScale().get(),client.isEnforceUnicode()));
        int anchorHeight=ChatComponent.getHeight(client.options.chatHeightUnfocused().get());
        int originY=Math.max(0,screenHeight-40-anchorHeight);
        layout.recordNative("chat",new Rect(0,originY,getWidth()+8,anchorHeight));
        var position=layout.position("chat",client.getWindow().getGuiScaledWidth(),screenHeight);
        float scale=layout.scale("chat");
        vela$x=position.x();vela$y=position.y();vela$scale=scale;vela$originY=originY;vela$moved=true;
        g.pose().translate(vela$x,vela$y,0);g.pose().scale(scale,scale,1);g.pose().translate(0,-originY,0);
    }
    @Inject(method="render",at=@At("RETURN"))
    private void vela$chatPoseEnd(GuiGraphics g,int ticks,int mouseX,int mouseY,boolean focused,CallbackInfo ci){g.pose().popPose();}

    @ModifyVariable(method="screenToChatX",at=@At("HEAD"),argsOnly=true)
    private double vela$chatX(double x){return vela$moved&&vela$scale>0?(x-vela$x)/vela$scale:x;}
    @ModifyVariable(method="screenToChatY",at=@At("HEAD"),argsOnly=true)
    private double vela$chatY(double y){return vela$moved&&vela$scale>0?(y-vela$y)/vela$scale+vela$originY:y;}
}
