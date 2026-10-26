package dev.sqwat.vela.mixin;

import dev.sqwat.vela.module.SelfTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
abstract class SelfTagMixin {
    @Inject(method="shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z",at=@At("HEAD"),cancellable=true)
    private void vela$selfTag(Avatar entity,double distance,CallbackInfoReturnable<Boolean> ci){
        var mc=Minecraft.getInstance();var services=dev.sqwat.vela.VelaClient.services();
        if(services!=null&&services.modules().byId("self_tag") instanceof SelfTag tag
            &&tag.visible(entity==mc.player,mc.options.getCameraType().isFirstPerson(),mc.gui.hud.isHidden()||!dev.sqwat.vela.ui.HudVisibility.allowed(mc)||entity.isInvisible(),entity.isAlive()))ci.setReturnValue(true);
    }
}
