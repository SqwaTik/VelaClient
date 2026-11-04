package dev.sqwat.vela.compat.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3x2fc;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * {@code guiRenderState.submitGuiElement(state)} on 1.21.5: the element is drawn straight away through a render type of
 * its pipeline, with its texture on sampler 0 and the current GUI layer's z.
 */
public final class GuiSubmit {
    private static final Map<RenderPipeline,RenderType> TYPES=new IdentityHashMap<>();
    private static final Map<RenderPipeline,Function<net.minecraft.resources.ResourceLocation,RenderType>> TEXTURED=new IdentityHashMap<>();
    private static GpuTexture texture;
    private static float z;
    private GuiSubmit(){}

    public static void submit(GuiGraphics g,GuiElementRenderState state){
        if(state==null)return;
        RenderType type=TYPES.computeIfAbsent(state.pipeline(),GuiSubmit::type);
        texture=state.textureSetup()==null?null:state.textureSetup().texture();
        z=Pose2D.z(g);
        try{g.drawSpecial(source->state.buildVertices(source.getBuffer(type)));}
        finally{texture=null;z=0;}
    }

    private static RenderType type(RenderPipeline pipeline){
        var shard=new RenderStateShard.EmptyTextureStateShard(()->{if(texture!=null)RenderSystem.setShaderTexture(0,texture);},()->{});
        return RenderType.create("vela_gui_"+pipeline.getLocation().getPath().replace('/','_'),786432,pipeline,
            RenderType.CompositeState.builder().setTextureState(shard).createCompositeState(false));
    }

    /** {@code g.blit(pipeline, id, ...)}: 1.21.5's blit takes a render type per texture instead of a pipeline. */
    public static Function<net.minecraft.resources.ResourceLocation,RenderType> textured(RenderPipeline pipeline){
        return TEXTURED.computeIfAbsent(pipeline,p->net.minecraft.Util.memoize(id->RenderType.create(
            "vela_gui_"+p.getLocation().getPath().replace('/','_'),786432,p,RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(id,net.minecraft.util.TriState.FALSE,false)).createCompositeState(false))));
    }

    /** {@code out.addVertexWith2DPose(pose, x, y)}. */
    public static VertexConsumer vertex(VertexConsumer out,Matrix3x2fc pose,float x,float y){
        return out.addVertex(pose.m00()*x+pose.m10()*y+pose.m20(),pose.m01()*x+pose.m11()*y+pose.m21(),z);
    }

    /** {@code rectangle.transformMaxBounds(pose)}: the screen box around the transformed rectangle. */
    public static ScreenRectangle bounds(ScreenRectangle r,Matrix3x2fc pose){
        float[] xs=new float[4],ys=new float[4];
        int[][] corners={{r.left(),r.top()},{r.right(),r.top()},{r.left(),r.bottom()},{r.right(),r.bottom()}};
        float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;
        for(int i=0;i<4;i++){
            float x=corners[i][0],y=corners[i][1];
            xs[i]=pose.m00()*x+pose.m10()*y+pose.m20();ys[i]=pose.m01()*x+pose.m11()*y+pose.m21();
            minX=Math.min(minX,xs[i]);minY=Math.min(minY,ys[i]);maxX=Math.max(maxX,xs[i]);maxY=Math.max(maxY,ys[i]);
        }
        int x=(int)Math.floor(minX),y=(int)Math.floor(minY);
        return new ScreenRectangle(x,y,(int)Math.ceil(maxX)-x,(int)Math.ceil(maxY)-y);
    }
}
