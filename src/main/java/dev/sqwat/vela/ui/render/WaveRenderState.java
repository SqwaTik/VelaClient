package dev.sqwat.vela.ui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

/** One submission for the visible annular mesh; no full-screen quad per wave. */
final class WaveRenderState implements GuiElementRenderState {
    private final Matrix3x2f pose;
    private final float[] vertices;
    private final TextureSetup texture;
    private final RenderPipeline pipeline;
    private final ScreenRectangle clip,bounds;
    private final int color;
    WaveRenderState(Matrix3x2f pose,float[] vertices,TextureSetup texture,RenderPipeline pipeline,int color,ScreenRectangle clip){
        this.pose=new Matrix3x2f(pose);this.vertices=vertices;this.texture=texture;this.pipeline=pipeline;this.color=color;this.clip=clip;
        float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;
        for(int i=0;i<vertices.length;i+=5){minX=Math.min(minX,vertices[i]);minY=Math.min(minY,vertices[i+1]);maxX=Math.max(maxX,vertices[i]);maxY=Math.max(maxY,vertices[i+1]);}
        int x=(int)Math.floor(minX),y=(int)Math.floor(minY);
        var box=new ScreenRectangle(x,y,(int)Math.ceil(maxX)-x,(int)Math.ceil(maxY)-y).transformMaxBounds(this.pose);
        bounds=clip==null?box:clip.intersection(box);
    }
    @Override public void buildVertices(VertexConsumer out){
        // Z carries reciprocal clip W; the wave vertex shader restores GUI depth.
        for(int i=0;i<vertices.length;i+=5)out.addVertex(pose.m00()*vertices[i]+pose.m10()*vertices[i+1]+pose.m20(),
            pose.m01()*vertices[i]+pose.m11()*vertices[i+1]+pose.m21(),vertices[i+4])
            .setUv(vertices[i+2],vertices[i+3]).setColor(color);
    }
    @Override public RenderPipeline pipeline(){return pipeline;}
    @Override public TextureSetup textureSetup(){return texture;}
    @Override public ScreenRectangle scissorArea(){return clip;}
    @Override public ScreenRectangle bounds(){return bounds;}
}
