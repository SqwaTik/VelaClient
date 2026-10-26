package dev.sqwat.vela.mixin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;
@Mixin(ItemStack.class)
abstract class ShulkerTooltipMixin {
    @Inject(method="addToTooltip",at=@At("HEAD"),cancellable=true)
    private void vela$omitContents(net.minecraft.core.component.DataComponentType<?> type,net.minecraft.world.item.Item.TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
        if(type==net.minecraft.core.component.DataComponents.CONTAINER&&dev.sqwat.vela.module.ShulkerPreview.tooltip((ItemStack)(Object)this)!=null)ci.cancel();
    }
    @Inject(method="getTooltipImage",at=@At("RETURN"),cancellable=true)
    private void vela$contents(CallbackInfoReturnable<Optional<TooltipComponent>> cir){
        if(cir.getReturnValue().isPresent())return;
        var preview=dev.sqwat.vela.module.ShulkerPreview.tooltip((ItemStack)(Object)this);
        if(preview!=null)cir.setReturnValue(Optional.of(preview));
    }
}
