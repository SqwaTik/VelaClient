package dev.sqwat.vela.mixin.screen;

import dev.sqwat.vela.module.utility.StreamerMode;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Only the font boundary is changed: messages, profiles and packets retain their real values. 1.21.5 draws straight from
 * drawInternal (no prepared text), so that is where strings and sequences are masked.
 */
@Mixin(Font.class)
abstract class StreamerFontMixin {
    @Shadow private int drawInternal(FormattedCharSequence text,float x,float y,int color,boolean shadow,Matrix4f pose,MultiBufferSource buffers,Font.DisplayMode mode,int background,int light,boolean inverseDepth){throw new AssertionError();}

    @ModifyVariable(method={"drawInternal(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I","drawInBatch8xOutline","width(Lnet/minecraft/util/FormattedCharSequence;)I"},at=@At("HEAD"),argsOnly=true)
    private FormattedCharSequence vela$sequence(FormattedCharSequence value){var mask=StreamerMode.mask();return mask==null?value:mask.sequence(value);}
    @ModifyVariable(method={"width(Lnet/minecraft/network/chat/FormattedText;)I","split","wordWrapHeight(Lnet/minecraft/network/chat/FormattedText;I)I","substrByWidth"},at=@At("HEAD"),argsOnly=true)
    private FormattedText vela$formatted(FormattedText value){var mask=StreamerMode.mask();return mask==null?value:mask.formatted(value);}
    @Inject(method="width(Ljava/lang/String;)I",at=@At("HEAD"),cancellable=true)
    private void vela$width(String value,CallbackInfoReturnable<Integer> ci){
        if(value==null)return;var mask=StreamerMode.mask();
        if(mask!=null&&(mask.next(value,0)>=0||value.indexOf('§')>=0))ci.setReturnValue(((Font)(Object)this).width(mask.sequence(value)));
    }
    @Inject(method="drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I",at=@At("HEAD"),cancellable=true)
    private void vela$text(String value,float x,float y,int color,boolean shadow,Matrix4f pose,MultiBufferSource buffers,Font.DisplayMode mode,int background,int light,boolean inverseDepth,CallbackInfoReturnable<Integer> ci){
        var mask=StreamerMode.mask();
        if(value!=null&&mask!=null&&(mask.next(value,0)>=0||value.indexOf('§')>=0))
            ci.setReturnValue(drawInternal(mask.sequence(value),x,y,color,shadow,pose,buffers,mode,background,light,inverseDepth));
    }
}
