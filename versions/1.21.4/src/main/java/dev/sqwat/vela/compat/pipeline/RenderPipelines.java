package dev.sqwat.vela.compat.pipeline;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.CoreShaders;

/** The 1.21.5 pipelines and snippets Vela uses, over 1.21.4's vanilla shaders. */
public final class RenderPipelines {
    public static final RenderPipeline.Snippet GUI_SNIPPET=new RenderPipeline.Snippet(DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,CoreShaders.POSITION_COLOR,BlendFunction.TRANSLUCENT,false);
    public static final RenderPipeline.Snippet GUI_TEXTURED_SNIPPET=new RenderPipeline.Snippet(DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,CoreShaders.POSITION_TEX_COLOR,BlendFunction.TRANSLUCENT,false);
    public static final RenderPipeline.Snippet DEBUG_FILLED_SNIPPET=new RenderPipeline.Snippet(DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,CoreShaders.POSITION_COLOR,BlendFunction.TRANSLUCENT,true);
    public static final RenderPipeline.Snippet LINES_SNIPPET=new RenderPipeline.Snippet(DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,CoreShaders.RENDERTYPE_LINES,BlendFunction.TRANSLUCENT,false);
    public static final RenderPipeline GUI=RenderPipeline.builder(GUI_SNIPPET).withLocation("pipeline/gui").build();
    public static final RenderPipeline GUI_TEXTURED=RenderPipeline.builder(GUI_TEXTURED_SNIPPET).withLocation("pipeline/gui_textured").build();
    public static final RenderPipeline LINES=RenderPipeline.builder(LINES_SNIPPET).withLocation("pipeline/lines").build();
    public static final RenderPipeline DEBUG_QUADS=RenderPipeline.builder(DEBUG_FILLED_SNIPPET).withLocation("pipeline/debug_quads").withCull(false).build();
    private RenderPipelines(){}
    public static RenderPipeline register(RenderPipeline pipeline){return pipeline;}
}
