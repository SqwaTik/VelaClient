package dev.sqwat.vela.mixin.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class SwingAnimationsMixin {
    @Inject(method="swingArm",at=@At("HEAD"),cancellable=true)
    private void vela$swing(float progress,PoseStack pose,int side,HumanoidArm arm,CallbackInfo ci){
        var services=dev.sqwat.vela.VelaClient.services();var player=Minecraft.getInstance().player;
        if(services==null||player==null||!(services.modules().byId("swing_animations") instanceof dev.sqwat.vela.module.visual.SwingAnimations module))return;
        if(arm!=player.getMainArm())return;
        var item=player.getMainHandItem();
        if(item.is(net.minecraft.world.item.Items.CROSSBOW)||item.is(net.minecraft.world.item.Items.TRIDENT))return;
        boolean editor=services.modules().byId("view_model") instanceof dev.sqwat.vela.module.visual.ViewModel model&&model.previewing();
        var mc=Minecraft.getInstance();
        boolean preview=module.previewEnabled()&&mc.gui.screen() instanceof dev.sqwat.vela.ui.screen.VelaDashboardScreen screen&&screen.isModuleSettingsOpen("swing_animations");
        boolean aimed=mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit&&hit.getEntity() instanceof net.minecraft.world.entity.LivingEntity;
        if(!module.applies(item.is(ItemTags.SWORDS),player.isUsingItem(),editor,aimed||preview)){module.reset();return;}
        float ticks=6;
        var haste=player.getEffect(net.minecraft.world.effect.MobEffects.HASTE);
        var fatigue=player.getEffect(net.minecraft.world.effect.MobEffects.MINING_FATIGUE);
        if(haste!=null)ticks=Math.max(1,6-(1+haste.getAmplifier()));
        else if(fatigue!=null)ticks=6+(1+fatigue.getAmplifier())*2;
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        module.apply(pose,arm==HumanoidArm.RIGHT,module.progress(progress,player.tickCount+partial,ticks,preview));ci.cancel();
    }
}
