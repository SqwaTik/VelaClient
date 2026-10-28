package dev.sqwat.vela.mixin.entity;
import dev.sqwat.vela.module.visual.HitColor;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EquipmentLayerRenderer.class)
abstract class HitColorArmorMixin {
    @ModifyArgs(method="renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void vela$armor(Args args){
        var module=HitColor.current();
        if(module==null||!module.armor()||!(args.get(1) instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState state)
            ||!module.tints(state instanceof AvatarRenderState,state.hasRedOverlay,state.deathTime))return;
        args.set(6,net.minecraft.util.ARGB.multiply((Integer)args.get(6),module.color()));
    }
}
