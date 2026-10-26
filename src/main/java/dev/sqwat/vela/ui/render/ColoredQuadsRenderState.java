package dev.sqwat.vela.ui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

/** Textured quads with one colour per quad, submitted as a single ordered GUI element. Arrays are owned by the state. */
final class ColoredQuadsRenderState implements GuiElementRenderState {
    private final Matrix3x2f pose;
    private final float[] quads; // x0,y0,x1,y1,u0,v0,u1,v1 per quad
    private final int[] colors;
    private final int count;
    private final TextureSetup texture;
    private final ScreenRectangle clip,bounds;

    ColoredQuadsRenderState(Matrix3x2f pose,float[] quads,int[] colors,int count,TextureSetup texture,ScreenRectangle clip){
        this.pose=new Matrix3x2f(pose);this.quads=quads;this.colors=colors;this.count=count;this.texture=texture;this.clip=clip;
        float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;
        for(int q=0,i=0;q<count;q++,i+=8){
            minX=Math.min(minX,Math.min(quads[i],quads[i+2]));minY=Math.min(minY,Math.min(quads[i+1],quads[i+3]));
            maxX=Math.max(maxX,Math.max(quads[i],quads[i+2]));maxY=Math.max(maxY,Math.max(quads[i+1],quads[i+3]));
        }
        if(count==0){bounds=null;return;}
        int x=(int)Math.floor(minX),y=(int)Math.floor(minY);
        var area=new ScreenRectangle(x,y,(int)Math.ceil(maxX)-x,(int)Math.ceil(maxY)-y).transformMaxBounds(this.pose);
        bounds=clip==null?area:clip.intersection(area);
    }
    @Override public void buildVertices(VertexConsumer out){
        for(int q=0,i=0;q<count;q++,i+=8){
            int c=colors[q];
            vertex(out,quads[i],quads[i+1],quads[i+4],quads[i+5],c);
            vertex(out,quads[i],quads[i+3],quads[i+4],quads[i+7],c);
            vertex(out,quads[i+2],quads[i+3],quads[i+6],quads[i+7],c);
            vertex(out,quads[i+2],quads[i+1],quads[i+6],quads[i+5],c);
        }
    }
    private void vertex(VertexConsumer out,float x,float y,float u,float v,int color){out.addVertexWith2DPose(pose,x,y).setUv(u,v).setColor(color);}
    @Override public RenderPipeline pipeline(){return RenderPipelines.GUI_TEXTURED;}
    @Override public TextureSetup textureSetup(){return texture;}
    @Override public ScreenRectangle scissorArea(){return clip;}
    @Override public ScreenRectangle bounds(){return bounds;}
}
