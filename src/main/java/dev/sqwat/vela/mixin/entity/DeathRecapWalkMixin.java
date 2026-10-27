package dev.sqwat.vela.mixin.entity;
import dev.sqwat.vela.module.visual.DeathReplay;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/**
 * Ghost limbs ignore the frame's partial tick: the replay already sets the interpolated limb phase every frame, and
 * re-extrapolating it with the live partial tick made paused legs twitch.
 */
@Mixin(WalkAnimationState.class)
abstract class DeathRecapWalkMixin {
    @Shadow private float position;
    @Shadow private float speed;
    @Inject(method="position(F)F",at=@At("HEAD"),cancellable=true)
    private void vela$position(float partial,CallbackInfoReturnable<Float> cir){if(DeathReplay.isGhostWalk((WalkAnimationState)(Object)this))cir.setReturnValue(position);}
    @Inject(method="speed(F)F",at=@At("HEAD"),cancellable=true)
    private void vela$speed(float partial,CallbackInfoReturnable<Float> cir){if(DeathReplay.isGhostWalk((WalkAnimationState)(Object)this))cir.setReturnValue(speed);}
}
