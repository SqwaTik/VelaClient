package dev.sqwat.vela.mixin.world;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 1.21.11: setupFog returns the fog colour and writes its local FogData to the fog buffer, so the distances change before that write. */
@Mixin(FogRenderer.class)
public abstract class DistanceFogMixin {
    @Inject(method="setupFog",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void vela$distanceFog(net.minecraft.client.Camera camera,int distance,net.minecraft.client.DeltaTracker delta,float darkening,net.minecraft.client.multiplayer.ClientLevel level,CallbackInfoReturnable<org.joml.Vector4f> cir,@Local FogData fog){
        if(dev.sqwat.vela.module.utility.RenderTweaks.hide("distance_fog")){
            fog.renderDistanceStart=1_000_000;fog.renderDistanceEnd=1_000_001;
        }
        var fluid=camera.getFluidInCamera();
        boolean clear=(fluid==net.minecraft.world.level.material.FogType.WATER||fluid==net.minecraft.world.level.material.FogType.LAVA)
            &&dev.sqwat.vela.module.utility.RenderTweaks.hide("fluid_fog");
        if(clear&&camera.entity() instanceof net.minecraft.world.entity.LivingEntity living&&
            (living.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)||living.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)))clear=false;
        if(clear){fog.environmentalStart=1_000_000;fog.environmentalEnd=1_000_001;}
        dev.sqwat.vela.render.perf.FogCulling.fog(fog);
    }
}
