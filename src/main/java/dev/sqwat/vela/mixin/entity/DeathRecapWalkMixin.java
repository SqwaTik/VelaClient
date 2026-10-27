package dev.sqwat.vela.mixin.entity;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.sqwat.vela.module.visual.DeathReplay;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
/**
 * Ghost limbs ignore the frame's partial tick: the replay already sets the interpolated limb phase every frame, and
 * re-extrapolating it with the live partial tick made paused legs twitch. ModifyReturnValue keeps the hot path
 * allocation-free (called for every living entity every frame).
 */
@Mixin(WalkAnimationState.class)
abstract class DeathRecapWalkMixin {
    @Shadow private float position;
    @Shadow private float speed;
    @ModifyReturnValue(method="position(F)F",at=@At("RETURN"))
    private float vela$position(float value){return DeathReplay.isGhostWalk((WalkAnimationState)(Object)this)?position:value;}
    @ModifyReturnValue(method="speed(F)F",at=@At("RETURN"))
    private float vela$speed(float value){return DeathReplay.isGhostWalk((WalkAnimationState)(Object)this)?speed:value;}
}
