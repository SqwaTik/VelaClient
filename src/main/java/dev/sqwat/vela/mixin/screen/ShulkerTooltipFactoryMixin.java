package dev.sqwat.vela.mixin.screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ClientTooltipComponent.class)
interface ShulkerTooltipFactoryMixin {
    @Inject(method="create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",at=@At("HEAD"),cancellable=true)
    private static void vela$preview(TooltipComponent data,CallbackInfoReturnable<ClientTooltipComponent> cir){
        if(data instanceof dev.sqwat.vela.module.utility.ShulkerPreview.Contents contents)cir.setReturnValue(new dev.sqwat.vela.ui.render.ShulkerTooltip(contents));
    }
}
