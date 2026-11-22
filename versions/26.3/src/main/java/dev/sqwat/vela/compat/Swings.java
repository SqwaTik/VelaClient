package dev.sqwat.vela.compat;

import dev.sqwat.vela.mixin.entity.LivingSwingAccessor;
import dev.sqwat.vela.mixin.entity.SwingStateAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;

/** 26.3 replaced LivingEntity's attackAnim/swinging/swingingArm/swingTime fields with a SwingState. */
public final class Swings {
    private Swings() {}
    private static SwingStateAccessor state(LivingEntity e) { return (SwingStateAccessor) ((LivingSwingAccessor) e).vela$swingState(); }
    public static float anim(LivingEntity e) { return state(e).vela$animation(); }
    public static InteractionHand arm(LivingEntity e) {
        var swing = e.getCurrentSwing();
        return swing == null ? InteractionHand.MAIN_HAND : swing.hand();
    }
    public static int ticks(LivingEntity e) { return state(e).vela$ticks(); }
    /** Puts a recorded swing back on a replay ghost. */
    public static void restore(LivingEntity e, float anim, boolean swinging, InteractionHand arm, int ticks) {
        var s = state(e);
        var old = s.vela$currentSwing();
        s.vela$currentSwing(swinging ? new LivingEntity.SwingDescription(arm, old != null ? old.animation() : SwingAnimation.DEFAULT,
            old != null ? old.durationTicks() : SwingAnimation.DEFAULT.duration()) : null);
        s.vela$ticks(ticks);
        s.vela$animation(anim);
        s.vela$oldAnimation(anim);
    }
}
