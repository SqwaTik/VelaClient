package dev.sqwat.vela.ui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

/** Immutable screen-space textured quads with per-quad color, submitted as one element. */
public record SpriteBatchRenderState(float[] vertices,int[] colors,RenderPipeline pipeline,
        TextureSetup textureSetup,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
    public SpriteBatchRenderState(float[] vertices,int[] colors,RenderPipeline pipeline,TextureSetup texture,ScreenRectangle clip){
        this(vertices,colors,pipeline,texture,clip,bounds(vertices,clip));
    }
    private static ScreenRectangle bounds(float[] vertices,ScreenRectangle clip){
        float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;
        for(int i=0;i<vertices.length;i+=4){minX=Math.min(minX,vertices[i]);minY=Math.min(minY,vertices[i+1]);maxX=Math.max(maxX,vertices[i]);maxY=Math.max(maxY,vertices[i+1]);}
        if(vertices.length==0)return null;
        int x=(int)Math.floor(minX),y=(int)Math.floor(minY);
        var result=new ScreenRectangle(x,y,(int)Math.ceil(maxX)-x,(int)Math.ceil(maxY)-y);
        return clip==null?result:clip.intersection(result);
    }
    @Override public void buildVertices(VertexConsumer out){
        for(int i=0;i<vertices.length;i+=4)out.addVertex(vertices[i],vertices[i+1],0).setUv(vertices[i+2],vertices[i+3]).setColor(colors[i/16]);
    }
}
