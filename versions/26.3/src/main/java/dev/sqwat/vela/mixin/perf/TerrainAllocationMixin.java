package dev.sqwat.vela.mixin.perf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** 26.3 version: prepareChunkRenders no longer copies the view matrix, so only the layer array is shared. */
@Mixin(LevelRenderer.class)
public abstract class TerrainAllocationMixin {
    @Unique private static final ChunkSectionLayer[] vela$layers=ChunkSectionLayer.values();

    @WrapOperation(method="prepareChunkRenders",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;values()[Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"),require=1)
    private ChunkSectionLayer[] vela$reuseLayers(Operation<ChunkSectionLayer[]> original){
        // The vanilla loop only reads this private array.
        return dev.sqwat.vela.module.Optimization.fewerAllocationsOn()?vela$layers:original.call();
    }
}
