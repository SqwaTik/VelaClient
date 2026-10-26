package dev.sqwat.vela.mixin.camera;
import dev.sqwat.vela.module.visual.DeathReplay;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** While a Death Recap replay runs the camera is placed by the replay, detached so no hand is drawn. */
@Mixin(Camera.class)
abstract class DeathRecapCameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Shadow protected abstract void setPosition(net.minecraft.world.phys.Vec3 position);
    @Inject(method="alignWithEntity",at=@At("TAIL"))
    private void vela$replay(float partial,CallbackInfo ci){
        var position=DeathReplay.cameraPosition();
        if(position==null)return;
        detached=true;
        setRotation(DeathReplay.cameraYaw(),DeathReplay.cameraPitch());
        setPosition(position);
    }
}
