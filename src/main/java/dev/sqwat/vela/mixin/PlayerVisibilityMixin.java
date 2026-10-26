package dev.sqwat.vela.mixin;

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
        if(entity instanceof AbstractClientPlayer player&&player.isDeadOrDying()){
            var services=dev.sqwat.vela.VelaClient.services();
            if(services!=null&&dev.sqwat.vela.ui.HudVisibility.allowed(Minecraft.getInstance())
                &&services.modules().byId("kill_effect") instanceof dev.sqwat.vela.module.KillEffect effect&&effect.replacesCorpse(player)){
                ci.setReturnValue(false);return;
            }
        }
        if(dev.sqwat.vela.module.RenderTweaks.hide("holograms")
                &&(entity instanceof net.minecraft.world.entity.Display.TextDisplay
                ||entity instanceof net.minecraft.world.entity.decoration.ArmorStand stand
                &&stand.isInvisible()&&stand.hasCustomName())){
            ci.setReturnValue(false);return;
        }
        if(!(entity instanceof AbstractClientPlayer)||entity.isPassenger()||entity.isVehicle()||entity.isCurrentlyGlowing())return;
        var client=Minecraft.getInstance();var camera=client.gameRenderer.mainCamera();
        if(entity==client.player||camera.isPanoramicMode()||camera.getCapturedFrustum()!=null||client.shouldEntityAppearGlowing(entity))return;
        // Generous cape/equipment/nameplate margin. Never retain a visibility result across frames.
        var box=dev.sqwat.vela.render.AutomaticVisibility.playerBounds(entity);
        if(!box.contains(x,y,z)&&!frustum.isVisible(box))ci.setReturnValue(false);
    }
}
