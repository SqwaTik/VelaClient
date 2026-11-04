package dev.sqwat.vela.mixin.screen;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.sqwat.vela.module.visual.Animations;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.5: ChatComponent.render(graphics, ticks, mouseX, mouseY, focused). */
@Mixin(ChatComponent.class)
abstract class AnimatedChatMixin {
    @Shadow private int chatScrollbarPos;
    @Unique private long vela$message;
    @Inject(method="addMessage",at=@At("RETURN"))
    private void vela$message(CallbackInfo ci){vela$message=Animations.active("chat")?System.nanoTime():0;}
    @WrapMethod(method="render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V")
    private void vela$draw(GuiGraphics g,int ticks,int mx,int my,boolean focused,Operation<Void> original){
        float shift=Animations.active("chat")&&chatScrollbarPos==0&&net.minecraft.client.Minecraft.getInstance().screen==null&&vela$message!=0?8*Animations.remaining(System.nanoTime()-vela$message,180_000_000):0;
        g.pose().pushPose();try{g.pose().translate(0,shift,0);original.call(g,ticks,mx,my,focused);}finally{g.pose().popPose();}
    }
}
