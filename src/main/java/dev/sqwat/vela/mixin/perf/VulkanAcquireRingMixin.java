package dev.sqwat.vela.mixin.perf;

import com.mojang.blaze3d.vulkan.VulkanGpuSurface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * One acquire semaphore per submit that can be in flight (VulkanCommandEncoder waits two submits back): a semaphore is
 * only reused once the submit that waited on it has finished.
 */
@Mixin(VulkanGpuSurface.class)
public abstract class VulkanAcquireRingMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 2))
    private int vela$acquireSemaphores(int vanilla) {
        return 4;
    }
}
