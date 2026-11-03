package dev.sqwat.vela.mixin.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cheap rejection before skin/renderer selection; normal vanilla visibility remains authoritative. */
@Mixin(EntityRenderDispatcher.class)
abstract class PlayerVisibilityMixin {
    @Inject(method="shouldRender",at=@At("HEAD"),cancellable=true)
    private void vela$playersOutsideView(Entity entity,Frustum frustum,double x,double y,double z,CallbackInfoReturnable<Boolean> ci){
        if(entity instanceof net.minecraft.world.entity.LivingEntity player&&player.isDeadOrDying()){
            var services=dev.sqwat.vela.VelaClient.services();
            if(services!=null&&dev.sqwat.vela.ui.hud.HudVisibility.allowed(Minecraft.getInstance())
                &&services.modules().byId("kill_effect") instanceof dev.sqwat.vela.module.visual.KillEffect effect&&effect.replacesCorpse(player)){
                ci.setReturnValue(false);return;
            }
        }
        // The type first: this runs for every entity every frame, the setting lookup only for holograms.
        if((entity instanceof net.minecraft.world.entity.Display.TextDisplay
                ||entity instanceof net.minecraft.world.entity.decoration.ArmorStand stand
                &&stand.isInvisible()&&stand.hasCustomName())
                &&dev.sqwat.vela.module.utility.RenderTweaks.hide("holograms")){
            ci.setReturnValue(false);return;
        }
        if(!(entity instanceof AbstractClientPlayer)||entity.isPassenger()||entity.isVehicle()||entity.isCurrentlyGlowing())return;
        var client=Minecraft.getInstance();var camera=client.gameRenderer.mainCamera();
        if(entity==client.player||camera.isPanoramicMode()||camera.getCapturedFrustum()!=null||client.shouldEntityAppearGlowing(entity))return;
        // Generous cape/equipment/nameplate margin. Never retain a visibility result across frames.
        var box=dev.sqwat.vela.render.entity.AutomaticVisibility.playerBounds(entity);
        if(!box.contains(x,y,z)&&!frustum.isVisible(box))ci.setReturnValue(false);
    }
}
