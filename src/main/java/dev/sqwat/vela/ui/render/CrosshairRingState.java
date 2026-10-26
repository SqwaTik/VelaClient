package dev.sqwat.vela.ui.render;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;

public final class CrosshairRingState implements GuiElementRenderState {
    private final Matrix3x2f pose;
    private final float x,y,radius,width,sweep,density;
    private final int color;
    private final ScreenRectangle clip,bounds;
    public CrosshairRingState(Matrix3x2f pose,float x,float y,float radius,float width,float sweep,int color,float density,ScreenRectangle clip){
        this.pose=new Matrix3x2f(pose);this.x=x;this.y=y;this.radius=radius;this.width=width;this.sweep=sweep;this.color=color;this.density=density;this.clip=clip;
        float r=radius+width/2+1;int left=(int)Math.floor(x-r),top=(int)Math.floor(y-r);
        var rect=new ScreenRectangle(left,top,(int)Math.ceil(x+r)-left,(int)Math.ceil(y+r)-top).transformMaxBounds(this.pose);
        bounds=clip==null?rect:clip.intersection(rect);
    }
    @Override public void buildVertices(VertexConsumer out){CrosshairRing.emit((px,py,c)->out.addVertexWith2DPose(pose,px,py).setColor(c),x,y,radius,width,sweep,color,density);}
    @Override public RenderPipeline pipeline(){return RenderPipelines.GUI;}
    @Override public TextureSetup textureSetup(){return TextureSetup.noTexture();}
    @Override public ScreenRectangle scissorArea(){return clip;}
    @Override public ScreenRectangle bounds(){return bounds;}
}
