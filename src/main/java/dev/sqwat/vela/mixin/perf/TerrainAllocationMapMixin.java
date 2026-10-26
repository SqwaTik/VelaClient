package dev.sqwat.vela.mixin.perf;

import com.mojang.blaze3d.vertex.UberGpuBuffer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

/** Keep native buffer lifetime/locking, but avoid one map node per GPU allocation. */
@Mixin(UberGpuBuffer.class)
abstract class TerrainAllocationMapMixin {
    @Shadow @Final @Mutable private Map<?,?> allocationMap;
    @Inject(method="<init>",at=@At("RETURN"))
    private void vela$compactAllocations(CallbackInfo ci) {
        allocationMap=new Object2ObjectOpenHashMap<>(allocationMap);
    }
}
