package dev.sqwat.vela.mixin;

import dev.sqwat.vela.ui.NativeHudCapture;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
abstract class NativeHudBoundsMixin {
    @Inject(method="fill(IIIII)V",at=@At("HEAD"))
    private void vela$measureFill(int x,int y,int right,int bottom,int color,CallbackInfo ci){if(NativeHudCapture.active())NativeHudCapture.rectangle(x,y,right,bottom);}
    @Inject(method="text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",at=@At("HEAD"))
    private void vela$measureText(Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow,CallbackInfo ci){if(NativeHudCapture.active())NativeHudCapture.text(x,y,font.width(text),font.lineHeight);}
}
