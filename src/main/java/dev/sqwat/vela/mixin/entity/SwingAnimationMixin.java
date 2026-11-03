package dev.sqwat.vela.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class SwingAnimationMixin {
    /**
     * Hits on air and on mobs reset the attack cooldown, and vanilla then lowers the held item and raises it again.
     * That dip drowned out the custom swing (mining has no cooldown, so it looked right only there); while a custom
     * mode is on the item stays up. Switching items still plays the normal swap.
     */
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="tick",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    private float vela$noCooldownDip(float scale){
        var services=dev.sqwat.vela.VelaClient.services();
        return services!=null&&services.modules().byId("swing_animation") instanceof dev.sqwat.vela.module.visual.SwingAnimation module
            &&module.customMode()?1:scale;
    }
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
