package dev.sqwat.vela.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.3: the hands are drawn from a render state; the editor preview fills that state with its props. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class ViewModelMixin {
    @Shadow private void submitArmWithItem(PlayerRenderState player,FirstPersonHandsAndItemsRenderState state,float partial,float pitch,
        InteractionHand hand,float swing,ItemStack item,float equip,PoseStack pose,SubmitNodeCollector collector,int light){throw new AssertionError();}
    @Unique private ItemStack vela$previewTotem;

    @Inject(method="submitHandsWithItems",at=@At("HEAD"),cancellable=true)
    private void vela$previewBothHands(float partial,PoseStack pose,SubmitNodeCollector collector,PlayerRenderState player,
                                       FirstPersonHandsAndItemsRenderState state,CallbackInfo ci){
        var services=dev.sqwat.vela.VelaClient.services();
        if(services==null||!(services.modules().byId("view_model") instanceof dev.sqwat.vela.module.visual.ViewModel model)||!model.previewing())return;
        var mc=Minecraft.getInstance();var local=mc.player;
        if(local==null||player.avatarRenderState==null)return;
        if(vela$previewTotem==null)vela$previewTotem=new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING);
        for(var hand:InteractionHand.values()){
            var item=local.getItemInHand(hand);
            // Maps use a two-handed pose based on renderer state; use the editing prop instead.
            boolean previewProp=item.isEmpty()||item.has(net.minecraft.core.component.DataComponents.MAP_ID);
            var shown=previewProp?vela$previewTotem:item;
            boolean right=(hand==InteractionHand.MAIN_HAND?local.getMainArm():local.getMainArm().getOpposite())==HumanoidArm.RIGHT;
            var context=right?ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            mc.getItemModelResolver().updateForTopItem(hand==InteractionHand.MAIN_HAND?state.mainHandRenderState:state.offHandRenderState,
                shown,context,local.level(),local,context.ordinal());
            submitArmWithItem(player,state,partial,local.getXRot(),hand,0,shown,0,pose,collector,player.avatarRenderState.lightCoords);
        }
        ci.cancel();
    }

    @Inject(method="submitArmWithItem",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",ordinal=0,shift=At.Shift.AFTER))
    private void vela$handPose(PlayerRenderState player,FirstPersonHandsAndItemsRenderState state,float partial,float pitch,InteractionHand hand,
                               float swing,ItemStack item,float equip,PoseStack pose,SubmitNodeCollector collector,int light,CallbackInfo ci){
        var services=dev.sqwat.vela.VelaClient.services();
        if(services==null||player.avatarRenderState==null||!(services.modules().byId("view_model") instanceof dev.sqwat.vela.module.visual.ViewModel model))return;
        var main=player.avatarRenderState.mainArm;
        model.apply(pose,hand,(hand==InteractionHand.MAIN_HAND?main:main.getOpposite())==HumanoidArm.RIGHT);
    }
}
