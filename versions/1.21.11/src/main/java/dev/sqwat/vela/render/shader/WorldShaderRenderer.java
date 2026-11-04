package dev.sqwat.vela.render.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.sqwat.vela.module.visual.Shaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.SkyRenderState;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import java.util.OptionalInt;

/**
 * Render-thread-owned post pass, after the world and before hands/HUD. 1.21.11: the fog pass only. Wet surfaces write a
 * signed colour delta and depth to a half-float target, and 1.21.11's GPU API has no float texture format.
 */
public final class WorldShaderRenderer {
    // Fog is mix(scene,fog,a): fixed-function blending applies it in place, so it needs no scene copy.
    private static final RenderPipeline FOG=RenderPipelines.register(ShaderPipelines.fullscreen("atmosphere_fog","atmosphere_fog","atmosphere_fog")
        .withSampler("DepthSampler").withUniform("FogParams",UniformType.UNIFORM_BUFFER).withBlend(ShaderPipelines.MIX).build());
    private static GpuTextureView depthView;
    private static GpuBuffer fogUniforms;
    private static final Matrix4f inverse=new Matrix4f(),viewProjection=new Matrix4f();
    private static final Matrix4f projection=new Matrix4f();
    private static Object world;
    private static boolean failed;
    private static boolean fogReady;
    private WorldShaderRenderer(){}
    public static void initialize(){}
    public static void captureProjection(Matrix4f actual){projection.set(actual);}
    public static void render(ShaderCamera camera,SkyRenderState sky){
        HandShaderRenderer.updateLifecycle();
        var mc=Minecraft.getInstance();var settings=Shaders.current();
        if(settings==null||!settings.fogActive()||mc.level==null){close();failed=false;return;}
        if(world!=mc.level){close();world=mc.level;failed=false;}
        if(failed||camera.fogType!=FogType.NONE||camera.pos==null)return;
        var target=mc.getMainRenderTarget();
        var source=target.getColorTexture();var sourceDepth=target.getDepthTexture();
        if(source==null||sourceDepth==null||source.isClosed()||sourceDepth.isClosed())return;
        try{
            var device=RenderSystem.getDevice();
            if(!fogReady){
                if(!device.precompilePipeline(FOG).isValid())throw new IllegalStateException("Fog shader compilation failed");
                fogReady=true;
            }
            // The main depth buffer is created with texture-binding usage and is not attached to this pass: sample it directly.
            depthView=target.getDepthTextureView();
            if(depthView==null)return;
            if(fogUniforms==null)fogUniforms=device.createBuffer(()->"Vela fog parameters",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,128);
            var encoder=device.createCommandEncoder();
            viewProjection.set(projection).mul(camera.viewRotationMatrix);inverse.set(viewProjection).invert();
            try(var stack=MemoryStack.stackPush()){
                var data=stack.calloc(128);int rgb=settings.fogColor();
                data.putFloat(0,settings.fogDensity());data.putFloat(4,(System.nanoTime()%600_000_000_000L)/1e9f);
                data.putFloat(8,settings.fogFalloff());data.putFloat(12,settings.fogHeight());
                data.putFloat(16,1);data.putFloat(20,settings.fogHaze());data.putFloat(24,Math.max(128,camera.depthFar));
                // OpenGL depth is -1..1
                data.putFloat(28,0);
                data.putFloat(32,(float)camera.pos.x);data.putFloat(36,(float)camera.pos.y);data.putFloat(40,(float)camera.pos.z);
                data.putFloat(48,(rgb>>16&255)/255f);data.putFloat(52,(rgb>>8&255)/255f);data.putFloat(56,(rgb&255)/255f);data.putFloat(60,1);
                inverse.get(64,data);
                encoder.writeToBuffer(fogUniforms.slice(),data);
            }
            try(var pass=encoder.createRenderPass(()->"Vela fog",target.getColorTextureView(),OptionalInt.empty())){
                pass.setPipeline(FOG);
                pass.bindTexture("DepthSampler",depthView,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("FogParams",fogUniforms);pass.draw(0,6);
            }
        }catch(RuntimeException error){close();world=mc.level;failed=true;org.slf4j.LoggerFactory.getLogger("Vela").warn("World shaders disabled after GPU error",error);}
    }
    public static void close(){
        depthView=null; // owned by the main render target
        if(fogUniforms!=null)fogUniforms.close();fogUniforms=null;world=null;fogReady=false;
    }
}
