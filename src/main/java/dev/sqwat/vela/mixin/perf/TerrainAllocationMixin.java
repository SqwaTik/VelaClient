package dev.sqwat.vela.mixin.perf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Shares only immutable inputs within one terrain submission, never cached world geometry. */
@Mixin(LevelRenderer.class)
public abstract class TerrainAllocationMixin {
    @Unique private static final ChunkSectionLayer[] vela$layers=ChunkSectionLayer.values();

    @WrapOperation(method="prepareChunkRenders",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;values()[Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;"),require=2)
    private ChunkSectionLayer[] vela$reuseLayers(Operation<ChunkSectionLayer[]> original){
        // Both vanilla loops only read this private array.
        return dev.sqwat.vela.module.Optimization.fewerAllocationsOn()?vela$layers:original.call();
    }

    @WrapOperation(method="prepareChunkRenders",at=@At(value="NEW",target="(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;"))
    private Matrix4f vela$oneViewSnapshot(Matrix4fc source,Operation<Matrix4f> original,
            @Share("vela$terrainView") LocalRef<Matrix4f> snapshot){
        if(!dev.sqwat.vela.module.Optimization.fewerAllocationsOn())return original.call(source);
        // ChunkSectionInfo only reads this matrix while writing uniforms in this invocation.
        // Invocation-local ownership also covers reentry and exceptions without stale camera data.
        Matrix4f value=snapshot.get();
        if(value==null){value=original.call(source);snapshot.set(value);}
        return value;
    }
}
