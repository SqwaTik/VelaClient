package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.ui.ChatPrivacy;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Chat privacy: hidden characters of the chat input are drawn as spaces (the value itself is untouched), then a blur is
 * laid over where they are.
 */
@Mixin(EditBox.class)
abstract class ChatPrivacyMixin {
    @Shadow private String value;
    @Shadow private int displayPos;
    @Shadow private int textX;
    @Shadow private int textY;
    @Shadow @org.spongepowered.asm.mixin.Final private Font font;
    @ModifyVariable(method="applyFormat",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private String vela$mask(String text,String original,int offset){
        int from=ChatPrivacy.hiddenFrom((EditBox)(Object)this,value);
        return from<0?text:ChatPrivacy.mask(text,offset,from);
    }
    @Inject(method="extractWidgetRenderState",at=@At("TAIL"))
    private void vela$blur(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta,CallbackInfo ci){
        int from=ChatPrivacy.hiddenFrom((EditBox)(Object)this,value);
        if(from<0||displayPos>value.length())return;
        String shown=font.plainSubstrByWidth(value.substring(displayPos),((EditBox)(Object)this).getInnerWidth());
        int start=Math.max(0,from-displayPos);
        if(start>=shown.length())return;
        String tail=shown.substring(start);
        var hidden=new boolean[tail.length()];
        for(int i=0;i<hidden.length;i++)hidden[i]=tail.charAt(i)!=' ';
        int x0=textX+font.width(shown.substring(0,start)),cell=font.width(" ");
        ChatPrivacy.drawBlur(g,x0,x0+cell*tail.length(),textY,cell,hidden);
    }
}
