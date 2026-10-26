package dev.sqwat.vela.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import dev.sqwat.vela.module.Shaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import java.util.Optional;

/** Render-thread-owned post pass, after the world and before hands/HUD. */
public final class WorldShaderRenderer {
    // Fog is mix(scene,fog,a): fixed-function blending applies it in place, so it needs no scene copy.
    private static final RenderPipeline FOG=RenderPipelines.register(RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/atmosphere_fog"))
        .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/atmosphere_fog")).withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/atmosphere_fog"))
        .withBindGroupLayout(BindGroupLayout.builder().withSampler("DepthSampler").withUniform("FogParams",UniformType.UNIFORM_BUFFER).build())
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).withDepthStencilState(Optional.empty())
        .withColorTargetState(new ColorTargetState(new BlendFunction(com.mojang.blaze3d.platform.BlendFactor.SRC_ALPHA,com.mojang.blaze3d.platform.BlendFactor.ONE_MINUS_SRC_ALPHA,
            com.mojang.blaze3d.platform.BlendFactor.ZERO,com.mojang.blaze3d.platform.BlendFactor.ONE))).build());
    private static final RenderPipeline WET=pipeline("wet_surfaces","ReflectParams");
    private static final RenderPipeline WET_RESOLVE=RenderPipelines.register(RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/wet_resolve"))
        .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/wet_surfaces"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/wet_resolve"))
        .withBindGroupLayout(BindGroupLayout.builder().withSampler("Scene").withSampler("DepthSampler").withSampler("Reflection").build())
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).withDepthStencilState(Optional.empty()).withColorTargetState(ColorTargetState.DEFAULT).build());
    private static RenderPipeline pipeline(String name,String uniform){return RenderPipelines.register(RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/"+name))
        .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/"+name)).withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/"+name))
        .withBindGroupLayout(BindGroupLayout.builder().withSampler("Scene").withSampler("DepthSampler").withUniform(uniform,UniformType.UNIFORM_BUFFER).build())
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)
        .withDepthStencilState(Optional.empty()).withColorTargetState(name.equals("wet_surfaces")
            ?new ColorTargetState(Optional.empty(),com.mojang.blaze3d.GpuFormat.RGBA16_FLOAT,ColorTargetState.WRITE_ALL)
            :ColorTargetState.DEFAULT).build());}
    private static GpuTexture scene;
    private static GpuTextureView sceneView,depthView;
    private static GpuTexture reflection;
    private static GpuTextureView reflectionView;
    // Separate buffers: rewriting one buffer between two draws of a frame can force a driver sync.
    private static GpuBuffer uniforms,fogUniforms;
    private static final Matrix4f inverse=new Matrix4f(),viewProjection=new Matrix4f();
    private static final Matrix4f projection=new Matrix4f();
    private static Object world;
    private static boolean failed;
    private static boolean fogReady,wetReady;
    private WorldShaderRenderer(){}
    public static void initialize(){}
    public static void captureProjection(Matrix4f actual){projection.set(actual);}
    public static void render(CameraRenderState camera,net.minecraft.client.renderer.state.level.SkyRenderState sky){
        HandShaderRenderer.updateLifecycle();
        var mc=Minecraft.getInstance();var settings=Shaders.current();
        if(settings==null||(!settings.fogActive()&&!settings.wetActive())||mc.level==null){close();failed=false;return;}
        if(world!=mc.level){close();world=mc.level;failed=false;}
        if(failed||camera.fogType!=FogType.NONE||camera.pos==null)return;
        var target=mc.gameRenderer.mainRenderTarget();
        var source=target.getColorTexture();var sourceDepth=target.getDepthTexture();
        if(source==null||sourceDepth==null||source.isClosed()||sourceDepth.isClosed())return;
        try{
            var device=RenderSystem.getDevice();
            if(settings.fogActive()&&!fogReady){
                if(!device.precompilePipeline(FOG).isValid())throw new IllegalStateException("Fog shader compilation failed");
                fogReady=true;
            }
            if(settings.wetActive()&&!wetReady){
                if(!device.precompilePipeline(WET).isValid()||!device.precompilePipeline(WET_RESOLVE).isValid())throw new IllegalStateException("Wet surface shader compilation failed");
                wetReady=true;
            }
            // The main depth buffer is created with texture-binding usage and is not attached to these passes: sample it directly.
            depthView=target.getDepthTextureView();
            if(depthView==null)return;
            if(uniforms==null){
                uniforms=device.createBuffer(()->"Vela wet parameters",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,208);
                fogUniforms=device.createBuffer(()->"Vela fog parameters",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,128);
            }
            var encoder=device.createCommandEncoder();
            viewProjection.set(projection).mul(camera.viewRotationMatrix);inverse.set(viewProjection).invert();
            if(settings.wetActive()){
                // Wet resolve writes scene+signed delta, which blending cannot express: it needs one colour copy.
                if(scene==null||scene.getWidth(0)!=source.getWidth(0)||scene.getHeight(0)!=source.getHeight(0)||scene.getFormat()!=source.getFormat()){
                    releaseScene();
                    scene=device.createTexture("Vela wet scene",GpuTexture.USAGE_COPY_DST|GpuTexture.USAGE_TEXTURE_BINDING,source.getFormat(),source.getWidth(0),source.getHeight(0),1,1);
                    sceneView=device.createTextureView(scene);
                }
                encoder.copyTextureToTexture(source,scene,0,0,0,0,0,source.getWidth(0),source.getHeight(0));
                int divisor=settings.reflectionResolutionDivisor();
                int rw=Math.max(1,source.getWidth(0)/divisor),rh=Math.max(1,source.getHeight(0)/divisor);
                if(reflection==null||reflection.getWidth(0)!=rw||reflection.getHeight(0)!=rh){
                    releaseReflection();
                    reflection=device.createTexture("Vela wet reflection",ShaderTextureUsage.CLEARED_TARGET,com.mojang.blaze3d.GpuFormat.RGBA16_FLOAT,rw,rh,1,1);
                    reflectionView=device.createTextureView(reflection);
                    org.slf4j.LoggerFactory.getLogger("Vela").debug("Wet render target: {}x{}, {} trace steps",rw,rh,settings.reflectionSteps());
                }
                try(var stack=MemoryStack.stackPush()){
                    var data=stack.calloc(208);int rgb=sky.skyColor;
                    data.putFloat(0,(float)camera.pos.x);data.putFloat(4,(float)camera.pos.y);data.putFloat(8,(float)camera.pos.z);
                    data.putFloat(12,(System.nanoTime()%3_600_000_000_000L)/1e9f);
                    data.putFloat(16,(rgb>>16&255)/255f);data.putFloat(20,(rgb>>8&255)/255f);data.putFloat(24,(rgb&255)/255f);data.putFloat(28,settings.reflection());
                    data.putFloat(32,-(float)Math.sin(sky.sunAngle));data.putFloat(36,(float)Math.cos(sky.sunAngle));data.putFloat(44,settings.wetness());
                    data.putFloat(52,mc.level.dimensionType().hasSkyLight()?.45f:0);data.putFloat(56,settings.reflectionSteps());data.putFloat(60,.98f);
                    data.putFloat(64,24);data.putFloat(68,1);data.putFloat(72,1);data.putFloat(76,device.getDeviceInfo().isZZeroToOne()?1:0);
                    inverse.get(80,data);viewProjection.get(144,data);encoder.writeToBuffer(uniforms.slice(),data);
                }
                try(var pass=device.createCommandEncoder().createRenderPass(()->"Vela wet surfaces",reflectionView,Optional.empty())){
                    pass.setPipeline(WET);
                    pass.bindTexture("Scene",sceneView,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                    pass.bindTexture("DepthSampler",depthView,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                    pass.setUniform("ReflectParams",uniforms);pass.draw(6,1,0,0);
                }
                try(var pass=device.createCommandEncoder().createRenderPass(()->"Vela wet resolve",target.getColorTextureView(),Optional.empty())){
                    pass.setPipeline(WET_RESOLVE);
                    pass.bindTexture("Scene",sceneView,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
                    pass.bindTexture("DepthSampler",depthView,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                    pass.bindTexture("Reflection",reflectionView,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                    pass.draw(6,1,0,0);
                }
            }
            else {releaseReflection();releaseScene();}
            if(settings.fogActive()){
            try(var stack=MemoryStack.stackPush()){
                var data=stack.calloc(128);int rgb=settings.fogColor();
                data.putFloat(0,settings.fogDensity());data.putFloat(4,(System.nanoTime()%600_000_000_000L)/1e9f);
                data.putFloat(8,settings.fogFalloff());data.putFloat(12,settings.fogHeight());
                data.putFloat(16,1);data.putFloat(20,settings.fogHaze());data.putFloat(24,Math.max(128,camera.depthFar));
                data.putFloat(28,device.getDeviceInfo().isZZeroToOne()?1:0);
                data.putFloat(32,(float)camera.pos.x);data.putFloat(36,(float)camera.pos.y);data.putFloat(40,(float)camera.pos.z);
                data.putFloat(48,(rgb>>16&255)/255f);data.putFloat(52,(rgb>>8&255)/255f);data.putFloat(56,(rgb&255)/255f);data.putFloat(60,1);
                inverse.get(64,data);
                encoder.writeToBuffer(fogUniforms.slice(),data);
            }
            try(var pass=device.createCommandEncoder().createRenderPass(()->"Vela fog",target.getColorTextureView(),Optional.empty())){
                pass.setPipeline(FOG);
                pass.bindTexture("DepthSampler",depthView,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                pass.setUniform("FogParams",fogUniforms);pass.draw(6,1,0,0);
            }
            }
        }catch(RuntimeException error){close();world=mc.level;failed=true;org.slf4j.LoggerFactory.getLogger("Vela").warn("World shaders disabled after GPU error",error);}
    }
    public static void close(){
        releaseReflection();releaseScene();
        depthView=null; // owned by the main render target
        if(uniforms!=null)uniforms.close();uniforms=null;
        if(fogUniforms!=null)fogUniforms.close();fogUniforms=null;world=null;fogReady=false;wetReady=false;
    }
    private static void releaseScene(){
        if(sceneView!=null)sceneView.close();sceneView=null;
        if(scene!=null)scene.close();scene=null;
    }
    private static void releaseReflection(){
        if(reflectionView!=null)reflectionView.close();reflectionView=null;
        if(reflection!=null)reflection.close();reflection=null;
    }
}
