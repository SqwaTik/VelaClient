package dev.sqwat.vela.ui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

/** One GPU scene snapshot shared by every visible glass panel. No CPU readback. */
public final class GlassSurface {
    private static final Identifier IMAGE=Identifier.fromNamespaceAndPath("velaclient","glass_scene");
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/glass"))
        .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/glass"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/glass")).build());
    private static Snapshot snapshot;
    private static final RenderPipeline WAVE=RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/shockwave"))
        .withCull(false)
        .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/shockwave"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/shockwave")).build());
    private static boolean requested,failed;
    private GlassSurface(){}
    public static void initialize(){} // Register shaders before resource reload.
    public static void wave(GuiGraphicsExtractor g,float[] mesh,int tint){
        if(failed||mesh.length==0)return;
        var client=Minecraft.getInstance();if(client.level==null)return;
        var source=client.gameRenderer.mainRenderTarget().getColorTexture();if(source==null)return;
        try{
            prepare(source);
            requested=true;
            var texture=net.minecraft.client.gui.render.TextureSetup.singleTexture(snapshot.getTextureView(),snapshot.getSampler());
            g.guiRenderState.addGuiElement(new WaveRenderState(g.pose(),mesh,texture,WAVE,tint,g.scissorStack.peek()));
        }catch(RuntimeException error){disable(error);}
    }
    private static void prepare(GpuTexture source){
        if(snapshot==null||snapshot.getTexture().isClosed()||snapshot.getTexture().getWidth(0)!=source.getWidth(0)||snapshot.getTexture().getHeight(0)!=source.getHeight(0)){
            snapshot=new Snapshot(source);
            Minecraft.getInstance().getTextureManager().register(IMAGE,snapshot);
        }
    }
    public static void draw(GuiGraphicsExtractor g,float x,float y,float width,float height,float opacity){
        if(opacity<.005f||width<=0||height<=0||failed)return;
        var client=Minecraft.getInstance();if(client.level==null)return;
        var source=client.gameRenderer.mainRenderTarget().getColorTexture();if(source==null)return;
        try{
            prepare(source);
            requested=true;
            g.pose().pushMatrix();
            try{
                g.pose().translate(x,y);g.pose().scale(width,height);
                g.blit(PIPELINE,IMAGE,0,0,0,0,1,1,1,1,1,1,(Math.round(255*Math.min(1,opacity))<<24)|(VelaTheme.PRIMARY&0xFFFFFF));
            }finally{g.pose().popMatrix();}
        }catch(RuntimeException error){disable(error);}
    }
    public static void capture(){
        if(!requested||snapshot==null||failed)return;requested=false;
        var source=Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTexture();
        if(source==null||source.isClosed()||snapshot.getTexture().isClosed())return;
        if(source.getWidth(0)!=snapshot.getTexture().getWidth(0)||source.getHeight(0)!=snapshot.getTexture().getHeight(0))return;
        try{RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(source,snapshot.getTexture(),0,0,0,0,0,source.getWidth(0),source.getHeight(0));}
        catch(RuntimeException error){disable(error);}
    }
    private static void disable(RuntimeException error){failed=true;requested=false;org.slf4j.LoggerFactory.getLogger("Vela").warn("Glass rendering disabled after GPU resource failure",error);}
    public static void close(){if(snapshot!=null){snapshot.close();snapshot=null;}requested=false;}
    private static final class Snapshot extends AbstractTexture {
        Snapshot(GpuTexture source){
            texture=RenderSystem.getDevice().createTexture("Vela glass scene",GpuTexture.USAGE_COPY_DST|GpuTexture.USAGE_TEXTURE_BINDING,source.getFormat(),source.getWidth(0),source.getHeight(0),1,1);
            textureView=RenderSystem.getDevice().createTextureView(texture);
            sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }
    }
}
