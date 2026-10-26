package dev.sqwat.vela.mixin;
import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.ui.render.Rect;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The shared access layer transforms rendered text, hover tests and clickable text together. */
@Mixin(ChatComponent.class)
public abstract class ChatPositionMixin {
    @Shadow protected abstract int getWidth();
    @Shadow protected abstract int getHeight();
    @Inject(method="extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",at=@At("HEAD"))
    private void vela$chatPose(ChatComponent.ChatGraphicsAccess graphics,int screenHeight,int ticks,ChatComponent.DisplayMode mode,CallbackInfo ci){
        if(VelaClient.services()==null)return;
        var client=net.minecraft.client.Minecraft.getInstance();var layout=VelaClient.services().hudLayout();
        layout.viewport(client.getWindow().getGuiScale());
        int anchorHeight=ChatComponent.getHeight(client.options.chatHeightUnfocused().get());
        int originY=Math.max(0,screenHeight-40-anchorHeight);
        layout.recordNative("chat",new Rect(0,originY,getWidth()+8,anchorHeight));
        var position=layout.position("chat",client.getWindow().getGuiScaledWidth(),screenHeight);
        float scale=layout.scale("chat");
        graphics.updatePose(pose->{pose.translate(position.x(),position.y());pose.scale(scale);pose.translate(0,-originY);});
    }
}
