package dev.sqwat.vela.mixin.entity;
import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Death Recap restores recorded limb swing exactly instead of re-simulating it. */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationAccessor {
    @Accessor("position") void vela$position(float value);
    @Accessor("speed") void vela$speed(float value);
    @Accessor("speedOld") void vela$speedOld(float value);
}
