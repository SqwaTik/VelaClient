package dev.sqwat.vela.mixin;

import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A resolved framed map is submitted by MapRenderer; its item model is never submitted. */
@Mixin(ItemFrameRenderer.class)
public abstract class MapFramePreparationMixin {
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
        method="extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/decoration/ItemFrame;getItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack vela$hidePicture(ItemStack original,ItemFrame frame,
            net.minecraft.client.renderer.entity.state.ItemFrameRenderState state,float partialTicks){
        return dev.sqwat.vela.module.RenderTweaks.hide("pictures")&&frame.getFramedMapId(original)!=null?ItemStack.EMPTY:original;
    }
    @Redirect(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForNonLiving(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/Entity;)V"))
    private void vela$prepareVisibleItem(ItemModelResolver resolver, ItemStackRenderState state,
                                       ItemStack stack, ItemDisplayContext context, Entity owner) {
        if (owner instanceof ItemFrame frame && !stack.isEmpty()) {
            var mapId = frame.getFramedMapId(stack);
            if (mapId != null && frame.level().getMapData(mapId) != null) {
                state.clear();
                return;
            }
        }
        resolver.updateForNonLiving(state, stack, context, owner);
    }
}
