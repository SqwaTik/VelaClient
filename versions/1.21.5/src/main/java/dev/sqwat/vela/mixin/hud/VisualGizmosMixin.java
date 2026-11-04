package dev.sqwat.vela.mixin.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.21.5 has no gizmos: Vela's effects are collected and drawn right after the translucent pass's debug renderers. */
@Mixin(DebugRenderer.class)
abstract class VisualGizmosMixin {
    @Inject(method="renderAfterTranslucents",at=@At("TAIL"))
    private void vela$visuals(PoseStack poseStack,MultiBufferSource.BufferSource buffers,double x,double y,double z,CallbackInfo ci){
        var services=dev.sqwat.vela.VelaClient.services();
        if(services==null)return;
        var circle=services.modules().byId("jump_circle");
        var hat=services.modules().byId("china_hat");
        var target=services.modules().byId("target_esp");
        var trail=services.modules().byId("dash_trail");
        var kill=services.modules().byId("kill_effect");
        var hitboxes=services.modules().byId("hitboxes");
        var trajectories=services.modules().byId("trajectories");
        var mace=dev.sqwat.vela.module.visual.MaceSmash.current();
        var blast=services.modules().byId("explosion_wave");
        boolean storage=dev.sqwat.vela.marker.MarkerRuntime.hasHighlights(services.markers());
        if(!storage&&(circle==null||!circle.enabled())&&(hat==null||!hat.enabled())&&(target==null||!target.enabled())&&(trail==null||!trail.enabled())&&(kill==null||!kill.enabled())&&(hitboxes==null||!hitboxes.enabled())&&(trajectories==null||!trajectories.enabled())&&(mace==null||!mace.active())&&(blast==null||!blast.enabled()))return;
        dev.sqwat.vela.compat.gizmos.Gizmos.begin();
        try{
            if(circle instanceof dev.sqwat.vela.module.visual.JumpCircle effect&&effect.enabled())effect.submit();
            if(hat instanceof dev.sqwat.vela.module.visual.ChinaHat effect&&effect.enabled())effect.submit();
            if(target instanceof dev.sqwat.vela.module.visual.TargetESP effect&&effect.enabled())effect.submit();
            if(trail instanceof dev.sqwat.vela.module.visual.DashTrail effect&&effect.enabled())effect.submit();
            if(kill instanceof dev.sqwat.vela.module.visual.KillEffect effect&&effect.enabled())effect.submit();
            if(hitboxes instanceof dev.sqwat.vela.module.visual.Hitboxes effect&&effect.enabled())effect.submit();
            if(trajectories instanceof dev.sqwat.vela.module.utility.Trajectories effect&&effect.enabled())effect.submit();
            if(storage)dev.sqwat.vela.marker.MarkerRuntime.submitHighlights(services.markers());
            if(mace!=null&&mace.active())mace.submit();
            if(blast instanceof dev.sqwat.vela.module.visual.ExplosionWave effect&&effect.enabled())effect.submit();
        }finally{dev.sqwat.vela.compat.gizmos.Gizmos.draw(poseStack,buffers,x,y,z);}
    }
}
