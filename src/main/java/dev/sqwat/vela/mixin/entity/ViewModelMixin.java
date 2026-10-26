package dev.sqwat.vela.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ViewModelMixin {
    @Shadow private void submitArmWithItem(AbstractClientPlayer player,float partial,float pitch,InteractionHand hand,float swing,
        ItemStack item,float equip,PoseStack pose,SubmitNodeCollector collector,int light){throw new AssertionError();}
    @Unique private ItemStack vela$previewTotem;
    @Inject(method="submitHandsWithItems",at=@At("HEAD"),cancellable=true)
    private void vela$previewBothHands(float partial,PoseStack pose,SubmitNodeCollector collector,
        net.minecraft.client.player.LocalPlayer player,int light,CallbackInfo ci){
        var services=dev.sqwat.vela.VelaClient.services();
        if(services==null||!(services.modules().byId("view_model") instanceof dev.sqwat.vela.module.visual.ViewModel model)||!model.previewing())return;
        if(vela$previewTotem==null)vela$previewTotem=new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
        for(var hand:InteractionHand.values()){
            var item=player.getItemInHand(hand);
            // Maps use a two-handed pose based on renderer state; use the editing prop instead.
            boolean previewProp=item.isEmpty()||item.has(net.minecraft.core.component.DataComponents.MAP_ID);
            submitArmWithItem(player,partial,player.getXRot(),hand,0,previewProp?vela$previewTotem:item,0,pose,collector,light);
        }
        ci.cancel();
    }
    @Inject(method="submitArmWithItem",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",ordinal=0,shift=At.Shift.AFTER))
    private void vela$handPose(AbstractClientPlayer player,float partial,float pitch,InteractionHand hand,float swing,
        ItemStack item,float equip,PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci){
        var services=dev.sqwat.vela.VelaClient.services();
        if(services!=null&&services.modules().byId("view_model") instanceof dev.sqwat.vela.module.visual.ViewModel model)
            model.apply(pose,hand,(hand==InteractionHand.MAIN_HAND?player.getMainArm():player.getMainArm().getOpposite())==net.minecraft.world.entity.HumanoidArm.RIGHT);
    }
}
