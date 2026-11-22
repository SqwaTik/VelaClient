package dev.sqwat.vela.mixin.entity;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.SwingState.class)
public interface SwingStateAccessor {
    @Accessor("currentSwing") LivingEntity.SwingDescription vela$currentSwing();
    @Accessor("currentSwing") void vela$currentSwing(LivingEntity.SwingDescription swing);
    @Accessor("ticks") int vela$ticks();
    @Accessor("ticks") void vela$ticks(int ticks);
    @Accessor("animation") float vela$animation();
    @Accessor("animation") void vela$animation(float animation);
    @Accessor("oldAnimation") void vela$oldAnimation(float animation);
}
