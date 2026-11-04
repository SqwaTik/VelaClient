package dev.sqwat.vela.compat.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.sqwat.vela.compat.gpu.GpuTexture;
import dev.sqwat.vela.compat.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3x2fc;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * {@code guiRenderState.submitGuiElement(state)} on 1.21.4: the element is drawn straight away through a render type
 * made from its pipeline (see {@link RenderPipeline}), with its texture on sampler 0 and the current GUI layer's z.
 * Elements whose pipeline needs one of Vela's own shaders are skipped.
 */
public final class GuiSubmit {
    private static final Map<RenderPipeline,RenderType> TYPES=new IdentityHashMap<>();
    private static final Map<RenderPipeline,Function<ResourceLocation,RenderType>> TEXTURED=new IdentityHashMap<>();
    private static GpuTexture texture;
    private static float z;
    private GuiSubmit(){}

    public static void submit(GuiGraphics g,GuiElementRenderState state){
        if(state==null||!state.pipeline().supported())return;
        RenderType type=TYPES.computeIfAbsent(state.pipeline(),GuiSubmit::type);
        texture=state.textureSetup()==null?null:state.textureSetup().texture();
        z=Pose2D.z(g);
        try{g.drawSpecial(source->state.buildVertices(source.getBuffer(type)));}
        finally{texture=null;z=0;}
    }

    private static RenderType type(RenderPipeline pipeline){
        var shard=new RenderStateShard.EmptyTextureStateShard(()->{if(texture!=null)RenderSystem.setShaderTexture(0,texture.id());},()->{});
        return RenderPipeline.create(name(pipeline),786432,pipeline,RenderType.CompositeState.builder().setTextureState(shard));
    }

    /** {@code g.blit(pipeline, id, ...)}: 1.21.4's blit takes a render type per texture instead of a pipeline. */
    public static Function<ResourceLocation,RenderType> textured(RenderPipeline pipeline){
        return TEXTURED.computeIfAbsent(pipeline,p->net.minecraft.Util.memoize(id->RenderPipeline.create(name(p),786432,p,
            RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(id,net.minecraft.util.TriState.FALSE,false)))));
    }

    private static String name(RenderPipeline pipeline){return "vela_gui_"+pipeline.getLocation().getPath().replace('/','_');}

    /** {@code out.addVertexWith2DPose(pose, x, y)}. */
    public static VertexConsumer vertex(VertexConsumer out,Matrix3x2fc pose,float x,float y){
        return out.addVertex(pose.m00()*x+pose.m10()*y+pose.m20(),pose.m01()*x+pose.m11()*y+pose.m21(),z);
    }

    /** {@code rectangle.transformMaxBounds(pose)}: the screen box around the transformed rectangle. */
    public static ScreenRectangle bounds(ScreenRectangle r,Matrix3x2fc pose){
        float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;
        int[][] corners={{r.left(),r.top()},{r.right(),r.top()},{r.left(),r.bottom()},{r.right(),r.bottom()}};
        for(int[] corner:corners){
            float x=corner[0],y=corner[1];
            float sx=pose.m00()*x+pose.m10()*y+pose.m20(),sy=pose.m01()*x+pose.m11()*y+pose.m21();
            minX=Math.min(minX,sx);minY=Math.min(minY,sy);maxX=Math.max(maxX,sx);maxY=Math.max(maxY,sy);
        }
        int x=(int)Math.floor(minX),y=(int)Math.floor(minY);
        return new ScreenRectangle(x,y,(int)Math.ceil(maxX)-x,(int)Math.ceil(maxY)-y);
    }
}
