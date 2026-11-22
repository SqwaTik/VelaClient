package dev.sqwat.vela.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 26.3: the arm swing is drawn by FirstPersonHandsAndItemsRenderer; the cooldown dip is handled on FirstPersonHandsAndItems. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
abstract class SwingAnimationMixin {
    @Inject(method="swingArm",at=@At("HEAD"),cancellable=true)
    private void vela$swing(float progress,PoseStack pose,int side,HumanoidArm arm,CallbackInfo ci){
        var services=dev.sqwat.vela.VelaClient.services();var player=Minecraft.getInstance().player;
        if(services==null||player==null||!(services.modules().byId("swing_animation") instanceof dev.sqwat.vela.module.visual.SwingAnimation module))return;
        if(arm!=player.getMainArm())return;
        var item=player.getMainHandItem();
        if(item.is(net.minecraft.world.item.Items.CROSSBOW)||item.is(net.minecraft.world.item.Items.TRIDENT))return;
        boolean editor=services.modules().byId("view_model") instanceof dev.sqwat.vela.module.visual.ViewModel model&&model.previewing();
        var mc=Minecraft.getInstance();
        boolean preview=module.previewEnabled()&&mc.gui.screen() instanceof dev.sqwat.vela.ui.screen.VelaDashboardScreen screen&&screen.isModuleSettingsOpen("swing_animation");
        boolean aimed=mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit&&hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity;
        if(!module.applies(player.isUsingItem(),editor,aimed||preview)){module.reset();return;}
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        module.apply(pose,arm==HumanoidArm.RIGHT,module.progress(player.tickCount+partial,dev.sqwat.vela.module.visual.SwingAnimation.swingTicks(player),preview));ci.cancel();
    }
}
