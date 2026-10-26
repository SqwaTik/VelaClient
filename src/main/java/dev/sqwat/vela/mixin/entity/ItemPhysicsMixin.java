package dev.sqwat.vela.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.render.entity.ItemEntityRenderStateAccess;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
abstract class ItemPhysicsMixin {
    @Unique private final java.util.Map<ItemEntity,dev.sqwat.vela.render.entity.DroppedItemMotion> vela$motions=new java.util.WeakHashMap<>();
    @Unique private static boolean vela$itemPhysicsEnabled(){
        var services=VelaClient.services();var module=services==null?null:services.modules().byId("item_physics");
        return module!=null&&module.enabled();
    }

    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void vela$groundState(ItemEntity entity,ItemEntityRenderState state,float partialTick,CallbackInfo ci){
        boolean enabled=vela$itemPhysicsEnabled();
        var access=(ItemEntityRenderStateAccess)state;
        access.vela$setGrounded(enabled);
        if(enabled){var bounds=state.item.getModelBoundingBox();boolean volume=bounds.maxZ-bounds.minZ>.08&&bounds.maxX-bounds.minX>.08;access.vela$setTilt(vela$motions.computeIfAbsent(entity,e->new dev.sqwat.vela.render.entity.DroppedItemMotion()).sample(entity.tickCount,entity.onGround(),entity.isInWater(),volume,partialTick));}
        else vela$motions.remove(entity);
    }

    @Redirect(method="submit",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/item/ItemEntity;getSpin(FF)F"))
    private float vela$stopGroundSpin(float age,float offset,ItemEntityRenderState state,PoseStack poseStack,SubmitNodeCollector collector,CameraRenderState camera){
        return ((ItemEntityRenderStateAccess)state).vela$isGrounded()?offset:ItemEntity.getSpin(age,offset);
    }

    @Redirect(method="submit",at=@At(value="INVOKE",target="Lnet/minecraft/util/Mth;sin(D)F"))
    private float vela$removeGroundBob(double value,ItemEntityRenderState state,PoseStack poseStack,SubmitNodeCollector collector,CameraRenderState camera){
        return ((ItemEntityRenderStateAccess)state).vela$isGrounded()?-1.0F:net.minecraft.util.Mth.sin(value);
    }

    @Inject(method="submit",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/ItemEntityRenderer;submitMultipleFromCount(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ItemClusterRenderState;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/phys/AABB;)V"))
    private void vela$layFlat(ItemEntityRenderState state,PoseStack poseStack,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci){
        if(((ItemEntityRenderStateAccess)state).vela$isGrounded()){
            AABB bounds=state.item.getModelBoundingBox();
            float tilt=((ItemEntityRenderStateAccess)state).vela$getTilt();
            double lift=dev.sqwat.vela.render.entity.DroppedItemMotion.floorOffset(bounds.minY,bounds.maxY,bounds.minZ,bounds.maxZ,tilt);
            poseStack.translate(0.0D,lift+bounds.minY+.015,0.0D);
            poseStack.mulPose(Axis.XP.rotationDegrees(tilt));
        }
    }
}
