package dev.sqwat.vela.render;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.BlendFactor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import java.lang.Math;

/** Camera-facing additive sprites, shared by the bounded cosmetic effects. */
public final class WorldSprite {
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/world_sprite"))
        .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.SRC_ALPHA,BlendFactor.ONE,BlendFactor.ZERO,BlendFactor.ONE))).build());
    private final Matrix4f matrix=new Matrix4f();
    private Vec3 camera;
    private final Vector4f point=new Vector4f();
    private float focal;
    private final boolean batched;
    private final java.util.Map<Identifier,Batch> batches=new java.util.LinkedHashMap<>();
    private static final class Batch {
        float[] vertices=new float[256];int[] colors=new int[16];int count;
        void add(float x,float y,float radius,float aspect,float rotation,int color,float u,float v,float u1,float v1){
            if(count==colors.length){colors=java.util.Arrays.copyOf(colors,count*2);vertices=java.util.Arrays.copyOf(vertices,count*32);}
            colors[count]=color;float cos=(float)Math.cos(rotation),sin=(float)Math.sin(rotation);
            for(int corner=0;corner<4;corner++){
                float sx=corner<2?-radius/2:radius/2,sy=(corner==0||corner==3?-1:1)*radius*aspect/2;
                int index=count*16+corner*4;
                vertices[index]=x+sx*cos-sy*sin;vertices[index+1]=y+sx*sin+sy*cos;
                vertices[index+2]=corner<2?u:u1;vertices[index+3]=corner==0||corner==3?v:v1;
            }
            count++;
        }
    }
    public static void initialize(){}
    public WorldSprite(){this(false);}
    public WorldSprite(boolean batched){this.batched=batched;begin();}
    public void begin(){var c=Minecraft.getInstance().gameRenderer.mainCamera();camera=c.position();c.getViewRotationProjectionMatrix(matrix);
        for(var batch:batches.values())batch.count=0;
        var up=new Vector3f(0,1,0).rotate(c.rotation());
        focal=new Vector4f(up.x,up.y,up.z,0).mul(matrix).length();}
    public void draw(GuiGraphicsExtractor g,Identifier texture,int texWidth,int texHeight,Vec3 position,float size,float rotation,int color){
        drawRegion(g,texture,texWidth,texHeight,position,size,rotation,color,0,0,1,1);
    }
    public void drawRegion(GuiGraphicsExtractor g,Identifier texture,int texWidth,int texHeight,Vec3 position,float size,float rotation,int color,float u,float v,float u1,float v1){
        point.set((float)(position.x-camera.x),(float)(position.y-camera.y),(float)(position.z-camera.z),1).mul(matrix);
        if(point.w<.1f||point.z<-point.w)return;
        float x=(point.x/point.w+1)*g.guiWidth()/2f,y=(1-point.y/point.w)*g.guiHeight()/2f;
        float pixels=size*focal*g.guiHeight()/(2*point.w);
        if(!Float.isFinite(pixels)||pixels<.3f||pixels>g.guiHeight()*2||x+pixels<0||y+pixels<0||x-pixels>g.guiWidth()||y-pixels>g.guiHeight())return;
        if(batched){batches.computeIfAbsent(texture,key->new Batch()).add(x,y,pixels,(float)texHeight/texWidth,rotation,color,u,v,u1,v1);return;}
        g.pose().pushMatrix();
        try{g.pose().identity();g.pose().translate(x,y);g.pose().rotate(rotation);g.pose().scale(pixels/texWidth,pixels/texWidth);
            g.blit(PIPELINE,texture,-texWidth/2,-texHeight/2,0,0,texWidth,texHeight,texWidth,texHeight,texWidth,texHeight,color);
        }finally{g.pose().popMatrix();}
    }
    public void flush(GuiGraphicsExtractor g){
        var textures=Minecraft.getInstance().getTextureManager();
        for(var entry:batches.entrySet()){
            var batch=entry.getValue();if(batch.count==0)continue;
            var texture=textures.getTexture(entry.getKey());
            g.guiRenderState.addGuiElement(new dev.sqwat.vela.ui.render.SpriteBatchRenderState(
                java.util.Arrays.copyOf(batch.vertices,batch.count*16),java.util.Arrays.copyOf(batch.colors,batch.count),
                PIPELINE,net.minecraft.client.gui.render.TextureSetup.singleTexture(texture.getTextureView(),texture.getSampler()),g.scissorStack.peek()));
        }
        for(var batch:batches.values())batch.count=0;
    }
}
