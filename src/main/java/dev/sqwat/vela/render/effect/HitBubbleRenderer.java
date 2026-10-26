package dev.sqwat.vela.render.effect;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import dev.sqwat.vela.module.visual.HitBubbles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import java.util.Optional;
import java.lang.Math;

/** One scene copy and one bounded-rectangle pass for all visible hit waves. */
public final class HitBubbleRenderer {
    private static final int BYTES=80+HitBubbles.LIMIT*16;
    private static final RenderPipeline PIPELINE=RenderPipelines.register(RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/hit_bubbles"))
        .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/hit_bubbles"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/hit_bubbles"))
        .withBindGroupLayout(BindGroupLayout.builder().withSampler("Scene").withUniform("BubbleParams",UniformType.UNIFORM_BUFFER).build())
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).withDepthStencilState(Optional.empty())
        .withColorTargetState(ColorTargetState.DEFAULT).build());
    private static final Matrix4f projection=new Matrix4f(),viewProjection=new Matrix4f();
    private static final Vector4f point=new Vector4f();
    private static GpuTexture scene;
    private static GpuTextureView view;
    private static GpuBuffer uniforms;
    private static Object world;
    private static boolean failed,ready;
    private HitBubbleRenderer(){}
    public static void captureProjection(Matrix4f matrix){projection.set(matrix);}
    public static void render(CameraRenderState camera){
        var mc=Minecraft.getInstance();var services=dev.sqwat.vela.VelaClient.services();
        var module=services==null?null:services.modules().byId("hit_bubbles");
        if(!(module instanceof HitBubbles bubbles)||!bubbles.enabled()||bubbles.shockwave()||mc.level==null){close();failed=false;return;}
        if(world!=mc.level){close();failed=false;world=mc.level;}
        if(failed||camera.pos==null)return;
        long now=System.nanoTime();bubbles.expire(now);if(bubbles.count()==0)return;
        var target=mc.gameRenderer.mainRenderTarget();var source=target.getColorTexture();
        if(source==null||source.isClosed())return;
        int w=source.getWidth(0),h=source.getHeight(0);float aspect=(float)w/h;
        viewProjection.set(projection).mul(camera.viewRotationMatrix);
        try(var stack=MemoryStack.stackPush()){
            var data=stack.calloc(BYTES);int count=0;float left=1,bottom=1,right=0,top=0;
            float radius=bubbles.radius(),width=Math.clamp(radius*.4f,.06f,.22f),extent=radius+width*2;
            for(var wave:bubbles.waves()){
                float envelope=wave.envelope(now);if(envelope<=0)continue;
                var p=wave.position();point.set((float)(p.x-camera.pos.x),(float)(p.y-camera.pos.y),(float)(p.z-camera.pos.z),1).mul(viewProjection);
                if(point.w<=.001f)continue;
                float u=point.x/point.w*.5f+.5f,v=point.y/point.w*.5f+.5f;
                float rx=extent/aspect;
                if(u+rx<0||u-rx>1||v+extent<0||v-extent>1)continue;
                left=Math.min(left,u-rx);right=Math.max(right,u+rx);bottom=Math.min(bottom,v-extent);top=Math.max(top,v+extent);
                int offset=80+count*16;data.putFloat(offset,u);data.putFloat(offset+4,v);
                data.putFloat(offset+8,wave.progress(now)*radius);data.putFloat(offset+12,envelope);count++;
            }
            if(count==0)return;
            var device=RenderSystem.getDevice();
            if(!ready){if(!device.precompilePipeline(PIPELINE).isValid())throw new IllegalStateException("Hit Bubbles shader compilation failed");ready=true;}
            if(scene==null||scene.getWidth(0)!=w||scene.getHeight(0)!=h||scene.getFormat()!=source.getFormat()){
                releaseBuffers();
                scene=device.createTexture("Vela hit bubbles scene",GpuTexture.USAGE_COPY_DST|GpuTexture.USAGE_TEXTURE_BINDING,source.getFormat(),w,h,1,1);
                view=device.createTextureView(scene);
                uniforms=device.createBuffer(()->"Vela hit waves",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,BYTES);
            }
            data.putFloat(0,count);data.putFloat(4,aspect);data.putFloat(8,(now%60_000_000_000L)/1e9f);data.putFloat(12,bubbles.warp());
            data.putFloat(16,bubbles.saturation());data.putFloat(20,bubbles.tint());data.putFloat(24,bubbles.strength());data.putFloat(28,radius);
            putColor(data,32,bubbles.color(now,false));putColor(data,48,bubbles.color(now,true));
            data.putFloat(64,Math.max(0,left));data.putFloat(68,Math.max(0,bottom));data.putFloat(72,Math.min(1,right));data.putFloat(76,Math.min(1,top));
            var encoder=device.createCommandEncoder();
            encoder.copyTextureToTexture(source,scene,0,0,0,0,0,w,h);encoder.writeToBuffer(uniforms.slice(),data);
            try(var pass=encoder.createRenderPass(()->"Vela hit bubbles",target.getColorTextureView(),Optional.empty())){
                pass.setPipeline(PIPELINE);pass.setUniform("BubbleParams",uniforms);
                pass.bindTexture("Scene",view,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));pass.draw(6,1,0,0);
            }
        }catch(RuntimeException error){close();world=mc.level;failed=true;org.slf4j.LoggerFactory.getLogger("Vela").warn("Hit Bubbles disabled after GPU error",error);}
    }
    private static void putColor(java.nio.ByteBuffer data,int offset,int color){
        data.putFloat(offset,(color>>16&255)/255f);data.putFloat(offset+4,(color>>8&255)/255f);data.putFloat(offset+8,(color&255)/255f);data.putFloat(offset+12,1);
    }
    private static void releaseBuffers(){
        if(view!=null)view.close();view=null;if(scene!=null)scene.close();scene=null;if(uniforms!=null)uniforms.close();uniforms=null;
    }
    public static void close(){releaseBuffers();world=null;ready=false;}
}
