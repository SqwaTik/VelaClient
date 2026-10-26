package dev.sqwat.vela.mixin;
import dev.sqwat.vela.module.Animations;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Mixin(ChatComponent.class)
abstract class AnimatedChatMixin {
    @Shadow private int chatScrollbarPos;
    @Unique private long vela$message;
    @Inject(method="addMessage",at=@At("RETURN"))
    private void vela$message(CallbackInfo ci){vela$message=Animations.active("chat")?System.nanoTime():0;}
    @WrapMethod(method="extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V")
    private void vela$draw(GuiGraphicsExtractor g,Font font,int ticks,int mx,int my,ChatComponent.DisplayMode mode,boolean flag,Operation<Void> original){
        float shift=Animations.active("chat")&&chatScrollbarPos==0&&net.minecraft.client.Minecraft.getInstance().gui.screen()==null&&vela$message!=0?8*Animations.remaining(System.nanoTime()-vela$message,180_000_000):0;
        g.pose().pushMatrix();try{g.pose().translate(0,shift);original.call(g,font,ticks,mx,my,mode,flag);}finally{g.pose().popMatrix();}
    }
}
