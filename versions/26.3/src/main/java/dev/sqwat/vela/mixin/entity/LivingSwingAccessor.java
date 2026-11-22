package dev.sqwat.vela.mixin.entity;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 26.3: the swing lives in {@link LivingEntity.SwingState}; the death replay records and restores it. */
@Mixin(LivingEntity.class)
public interface LivingSwingAccessor {
    @Accessor("swingState") LivingEntity.SwingState vela$swingState();
}
