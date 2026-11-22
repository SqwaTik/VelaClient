package dev.sqwat.vela.render.shader;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

/** 1.21.11: full-screen passes name their samplers and uniform blocks on the builder and draw six vertices from gl_VertexID. */
public final class ShaderPipelines {
    /** Premultiplied alpha over the target. */
    public static final BlendFunction PREMULTIPLIED=new BlendFunction(SourceFactor.ONE,DestFactor.ONE_MINUS_SRC_ALPHA,SourceFactor.ONE,DestFactor.ONE_MINUS_SRC_ALPHA);
    /** mix(target,colour,alpha), keeping the target's alpha. */
    public static final BlendFunction MIX=new BlendFunction(SourceFactor.SRC_ALPHA,DestFactor.ONE_MINUS_SRC_ALPHA,SourceFactor.ZERO,DestFactor.ONE);
    private ShaderPipelines(){}
    public static RenderPipeline.Builder fullscreen(String name,String vertex,String fragment){
        return RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/"+name))
            .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/"+vertex))
            .withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/"+fragment))
            .withCull(false).withDepthWrite(false).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withVertexFormat(DefaultVertexFormat.EMPTY,VertexFormat.Mode.TRIANGLES);
    }
}
