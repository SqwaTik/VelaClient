package dev.sqwat.vela.mixin.perf;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.sqwat.vela.render.perf.DistantChunks;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Each section compile knows whether it is a far one (see DistantChunks). */
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask")
public abstract class DistantCompileMixin {
    @WrapOperation(method = "doTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionCompiler;compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;"))
    private SectionCompiler.Results vela$distant(SectionCompiler compiler, SectionPos pos, RenderSectionRegion region, VertexSorting sorting,
                                                 SectionBufferBuilderPack buffers, Operation<SectionCompiler.Results> original) {
        DistantChunks.beginCompile(pos);
        try {
            return original.call(compiler, pos, region, sorting, buffers);
        } finally {
            DistantChunks.endCompile();
        }
    }
}
