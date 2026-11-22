package dev.sqwat.vela.mixin.screen;

import dev.sqwat.vela.module.utility.ShulkerPreview;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.5: Screen.renderWithTooltip draws the screen, then its deferred tooltip; the pinned preview goes between them. */
@Mixin(Screen.class)
abstract class ShulkerScreenMixin {
    @Inject(method="renderWithTooltip",at=@At("HEAD"))
    private void vela$frame(GuiGraphics g,int mx,int my,float partial,CallbackInfo ci){
        var p=ShulkerPreview.current();if(p==null)return;
        long window=net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
        p.frame(this,p.inspectHeld(window));
    }
    @Inject(method="renderWithTooltip",at=@At(value="FIELD",target="Lnet/minecraft/client/gui/screens/Screen;deferredTooltipRendering:Lnet/minecraft/client/gui/screens/Screen$DeferredTooltipRendering;",opcode=Opcodes.GETFIELD,ordinal=0))
    private void vela$pin(GuiGraphics g,int mx,int my,float partial,CallbackInfo ci){var p=ShulkerPreview.current();if(p!=null)p.drawPinned(g,mx,my);}
}
