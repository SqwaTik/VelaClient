package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.module.utility.ShulkerPreview;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
abstract class ShulkerScreenMixin {
    @Inject(method="extractRenderStateWithTooltipAndSubtitles",at=@At("HEAD"))
    private void vela$frame(GuiGraphicsExtractor g,int mx,int my,float partial,CallbackInfo ci){
        var p=ShulkerPreview.current();if(p==null)return;
        long window=net.minecraft.client.Minecraft.getInstance().getWindow().handle();
        p.frame(this,p.inspectHeld(window));
    }
    @Inject(method="extractRenderStateWithTooltipAndSubtitles",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V"))
    private void vela$pin(GuiGraphicsExtractor g,int mx,int my,float partial,CallbackInfo ci){var p=ShulkerPreview.current();if(p!=null)p.drawPinned(g,mx,my);}
}
