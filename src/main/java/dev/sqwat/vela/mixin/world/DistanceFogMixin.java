package dev.sqwat.vela.mixin.world;
import net.minecraft.client.renderer.fog.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FogRenderer.class)
public abstract class DistanceFogMixin {
    @Inject(method="setupFog",at=@At("RETURN"))
    private void vela$distanceFog(net.minecraft.client.Camera camera,int distance,net.minecraft.client.DeltaTracker delta,float darkening,net.minecraft.client.multiplayer.ClientLevel level,CallbackInfoReturnable<FogData> cir){
        if(dev.sqwat.vela.module.utility.RenderTweaks.hide("distance_fog")){
            var fog=cir.getReturnValue();fog.renderDistanceStart=1_000_000;fog.renderDistanceEnd=1_000_001;
        }
        var fluid=camera.getFluidInCamera();
        boolean clear=(fluid==net.minecraft.world.level.material.FogType.WATER||fluid==net.minecraft.world.level.material.FogType.LAVA)
            &&dev.sqwat.vela.module.utility.RenderTweaks.hide("fluid_fog");
        if(clear&&camera.entity() instanceof net.minecraft.world.entity.LivingEntity living&&
            (living.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)||living.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)))clear=false;
        if(clear){var fog=cir.getReturnValue();fog.environmentalStart=1_000_000;fog.environmentalEnd=1_000_001;}
        dev.sqwat.vela.render.perf.FogCulling.fog(cir.getReturnValue());
    }
}
