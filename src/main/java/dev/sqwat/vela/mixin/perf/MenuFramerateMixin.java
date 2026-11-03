package dev.sqwat.vela.mixin.perf;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla holds menus outside a world (the title screen, world and server lists) at 60 FPS. With the sleep-based
 * limiter that comes out uneven and the animated menus stutter; they get the frame limit set in the options instead.
 * The AFK and minimised-window limits stay.
 */
@Mixin(FramerateLimitTracker.class)
public abstract class MenuFramerateMixin {
    @Shadow private int framerateLimit;
    @Shadow public abstract FramerateLimitTracker.FramerateThrottleReason getThrottleReason();

    @Inject(method="getFramerateLimit",at=@At("HEAD"),cancellable=true)
    private void vela$menuLimit(CallbackInfoReturnable<Integer> cir){
        var reason=getThrottleReason();
        if(reason==FramerateLimitTracker.FramerateThrottleReason.OUT_OF_LEVEL_MENU
            ||reason==FramerateLimitTracker.FramerateThrottleReason.NONE)cir.setReturnValue(displayCap(framerateLimit));
    }

    /**
     * "Unlimited" (the slider's last step) renders hundreds of frames a second the screen never shows; on laptops
     * where the dedicated GPU hands every frame to the integrated one that is mostly heat and uneven delivery. Two
     * frames per refresh keeps a fresh frame for every refresh at a fraction of the load. The result stays under the
     * cutoff, since vanilla only runs its limiter for limits below it.
     */
    private static int displayCap(int limit){
        if(limit<net.minecraft.client.Options.UNLIMITED_FRAMERATE_CUTOFF)return limit;
        var optimization=dev.sqwat.vela.module.Optimization.current();
        if(optimization==null||!optimization.displayCap())return limit;
        int refresh=net.minecraft.client.Minecraft.getInstance().getWindow().getRefreshRate();
        return refresh>0?Math.min(net.minecraft.client.Options.UNLIMITED_FRAMERATE_CUTOFF-1,Math.max(60,refresh*2)):limit;
    }
}
