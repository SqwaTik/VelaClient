package dev.sqwat.vela.mixin;

import dev.sqwat.vela.module.StreamerMode;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only the font boundary is changed: messages, profiles and packets retain their real values. */
@Mixin(Font.class)
abstract class StreamerFontMixin {
    @ModifyVariable(method={"prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;","prepare8xTextOutline","width(Lnet/minecraft/util/FormattedCharSequence;)I"},at=@At("HEAD"),argsOnly=true)
    private FormattedCharSequence vela$sequence(FormattedCharSequence value){var mask=StreamerMode.mask();return mask==null?value:mask.sequence(value);}
    @ModifyVariable(method={"width(Lnet/minecraft/network/chat/FormattedText;)I","split","splitIgnoringLanguage","wordWrapHeight","substrByWidth"},at=@At("HEAD"),argsOnly=true)
    private FormattedText vela$formatted(FormattedText value){var mask=StreamerMode.mask();return mask==null?value:mask.formatted(value);}
    @Inject(method="width(Ljava/lang/String;)I",at=@At("HEAD"),cancellable=true)
    private void vela$width(String value,CallbackInfoReturnable<Integer> ci){
        if(value==null)return;var mask=StreamerMode.mask();
        if(mask!=null&&(mask.next(value,0)>=0||value.indexOf('§')>=0))ci.setReturnValue(((Font)(Object)this).width(mask.sequence(value)));
    }
    @Inject(method="prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",at=@At("HEAD"),cancellable=true)
    private void vela$text(String value,float x,float y,int color,boolean shadow,int background,CallbackInfoReturnable<Font.PreparedText> ci){
        var mask=StreamerMode.mask();
        if(mask!=null&&(mask.next(value,0)>=0||value.indexOf('§')>=0))ci.setReturnValue(((Font)(Object)this).prepareText(mask.sequence(value),x,y,color,shadow,false,background));
    }
}
