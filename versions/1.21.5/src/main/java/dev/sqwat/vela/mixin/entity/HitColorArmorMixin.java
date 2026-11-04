package dev.sqwat.vela.mixin.entity;

import dev.sqwat.vela.compat.ArmorTint;
import dev.sqwat.vela.module.visual.HitColor;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** 1.21.5: the armor layers draw straight to the model buffer; the state comes from {@link ArmorTint}. */
@Mixin(EquipmentLayerRenderer.class)
abstract class HitColorArmorMixin {
    @ModifyArg(method="renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/resources/ResourceLocation;)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),index=4)
    private int vela$armor(int color){
        var module=HitColor.current();var state=ArmorTint.current();
        if(module==null||state==null||!module.armor()||!module.tints(state instanceof PlayerRenderState,state.hasRedOverlay,state.deathTime))return color;
        return net.minecraft.util.ARGB.multiply(color,module.color());
    }
}
