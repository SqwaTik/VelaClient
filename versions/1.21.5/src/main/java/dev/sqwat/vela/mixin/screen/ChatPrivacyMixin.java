package dev.sqwat.vela.mixin.screen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.sqwat.vela.ui.ChatPrivacy;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

/**
 * Chat privacy: hidden characters of the chat input are drawn as spaces (the value itself is untouched), then a blur is
 * laid over where they are. 1.21.5 formats through the formatter function in renderWidget and keeps no text position.
 */
@Mixin(EditBox.class)
abstract class ChatPrivacyMixin {
    @Shadow private String value;
    @Shadow private int displayPos;
    @Shadow private boolean bordered;
    @Shadow @org.spongepowered.asm.mixin.Final private Font font;
    @WrapOperation(method="renderWidget",at=@At(value="INVOKE",target="Ljava/util/function/BiFunction;apply(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object vela$mask(BiFunction<Object,Object,Object> formatter,Object text,Object offset,Operation<Object> original){
        int from=ChatPrivacy.hiddenFrom((EditBox)(Object)this,value);
        return original.call(formatter,from<0?text:ChatPrivacy.mask((String)text,(Integer)offset,from),offset);
    }
    @Inject(method="renderWidget",at=@At("TAIL"))
    private void vela$blur(GuiGraphics g,int mouseX,int mouseY,float delta,CallbackInfo ci){
        int from=ChatPrivacy.hiddenFrom((EditBox)(Object)this,value);
        if(from<0||displayPos>value.length())return;
        var box=(EditBox)(Object)this;
        String shown=font.plainSubstrByWidth(value.substring(displayPos),box.getInnerWidth());
        int start=Math.max(0,from-displayPos);
        if(start>=shown.length())return;
        String tail=shown.substring(start);
        var hidden=new boolean[tail.length()];
        for(int i=0;i<hidden.length;i++)hidden[i]=tail.charAt(i)!=' ';
        int textX=bordered?box.getX()+4:box.getX(),textY=bordered?box.getY()+(box.getHeight()-8)/2:box.getY();
        int x0=textX+font.width(shown.substring(0,start)),cell=font.width(" ");
        ChatPrivacy.drawBlur(g,x0,x0+cell*tail.length(),textY,cell,hidden);
    }
}
