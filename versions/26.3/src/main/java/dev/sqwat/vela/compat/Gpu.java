package dev.sqwat.vela.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.util.UncheckedAutoCloseable;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

/**
 * 26.3 (renderpearl): render passes take compiled pipelines, and the device compiles asynchronously from a
 * {@link ShaderSource}. Pipelines go through vanilla's pipeline cache (the shaders come from the resource packs);
 * pipelines with their own sources (the custom sky shader) are compiled and kept here.
 */
public final class Gpu {
    private Gpu() {}
    private static final Map<RenderPipeline, CompiledRenderPipeline> CUSTOM = new IdentityHashMap<>();

    /** Shader text for a pipeline compiled from Vela's own sources. */
    @FunctionalInterface public interface Source { @Nullable String get(Identifier id, ShaderType type); }
    /** Stand-in for the old precompile result. */
    public record Result(boolean isValid) {}

    public static Result precompile(RenderPipeline pipeline) {
        if (CUSTOM.containsKey(pipeline)) return new Result(true);
        try { return new Result(RenderSystem.getCompiledPipelineNullable(pipeline) != null); }
        catch (RuntimeException error) { return new Result(false); }
    }

    public static Result precompile(RenderPipeline pipeline, Source source) {
        if (CUSTOM.containsKey(pipeline)) return new Result(true);
        var includes = ShaderManager.listAllIncludes(Minecraft.getInstance().getResourceManager());
        var shaders = new ShaderSource() {
            @Override public @Nullable String getShader(Identifier id, ShaderType type) { return source.get(id, type); }
            @Override public ShaderSource.@Nullable CachedIncludeSource getInclude(Identifier id) { return includes.get(id); }
            @Override public void close() { includes.values().forEach(ShaderSource.CachedIncludeSource::close); }
        };
        try (shaders) {
            var compiled = RenderSystem.getDevice().compilePipeline(pipeline, shaders, Util.backgroundExecutor()).join().finishCompile();
            if (compiled == null) return new Result(false);
            CUSTOM.put(pipeline, compiled);
            return new Result(true);
        }
    }

    public static CompiledRenderPipeline compiled(RenderPipeline pipeline) {
        var custom = CUSTOM.get(pipeline);
        return custom != null ? custom : RenderSystem.getCompiledPipeline(pipeline);
    }

    /** Drops the pipelines compiled from Vela's own sources. */
    public static void clearCustom() {
        CUSTOM.values().forEach(UncheckedAutoCloseable::close);
        CUSTOM.clear();
    }
}
