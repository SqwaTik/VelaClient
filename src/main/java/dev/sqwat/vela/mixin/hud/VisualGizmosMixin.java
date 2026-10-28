package dev.sqwat.vela.mixin.hud;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class VisualGizmosMixin {
    @Inject(method="finalizeGizmoCollection",at=@At("HEAD"))
    private void vela$visuals(CallbackInfo ci){
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
        try(var collection=((LevelRenderer)(Object)this).collectPerFrameRenderThreadGizmos()){
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
        }
    }
}
