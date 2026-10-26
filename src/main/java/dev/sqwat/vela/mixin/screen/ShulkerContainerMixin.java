package dev.sqwat.vela.mixin.screen;
import dev.sqwat.vela.module.utility.ShulkerPreview;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AbstractContainerScreen.class)
abstract class ShulkerContainerMixin {
    @Inject(method="extractTooltip",at=@At("HEAD"),cancellable=true)
    private void vela$tooltip(GuiGraphicsExtractor g,int mx,int my,CallbackInfo ci){var p=ShulkerPreview.current();if(p!=null&&p.pinned())ci.cancel();}
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void vela$click(MouseButtonEvent event,boolean doubled,CallbackInfoReturnable<Boolean> cir){var p=ShulkerPreview.current();if(p!=null&&p.contains(event.x(),event.y()))cir.setReturnValue(true);}
}
