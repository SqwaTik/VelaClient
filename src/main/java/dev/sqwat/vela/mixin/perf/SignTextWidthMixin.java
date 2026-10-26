package dev.sqwat.vela.mixin.perf;

import dev.sqwat.vela.render.perf.SignTextWidths;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractSignRenderer.class)
public abstract class SignTextWidthMixin {
    @Redirect(method="submitSignText",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/util/FormattedCharSequence;)I"))
    private int vela$reuseLineWidth(Font font,FormattedCharSequence line){
        int width=SignTextWidths.get(font,line);
        if(width<0){width=font.width(line);SignTextWidths.put(line,width);}
        return width;
    }
}
