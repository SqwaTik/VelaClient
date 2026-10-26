package dev.sqwat.vela.render.shader;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import dev.sqwat.vela.module.visual.Shaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import java.util.Optional;

/** Captures native hand submission once, preserving nested output overrides in finally. */
public final class HandShaderRenderer {
    private static final RenderPipeline HOLOGRAM=pipeline("hands_hologram",true),GLASS=pipeline("hands_glass",true),PLAIN=pipeline("hands_plain",false);
    private static RenderPipeline pipeline(String name,boolean effect){
        boolean glass=name.equals("hands_glass");
        var layout=BindGroupLayout.builder();
        if(glass)layout.withSampler("SceneTex").withSampler("HandTex").withUniform("GlassConfig",UniformType.UNIFORM_BUFFER);
        else{layout.withSampler("SceneSampler");
            if(effect)layout.withUniform("HandsHologramData",UniformType.UNIFORM_BUFFER);}
        return RenderPipelines.register(RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/"+name))
            .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/hands_hologram"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/"+name))
            .withBindGroupLayout(layout.build()).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false).withDepthStencilState(Optional.empty()).withColorTargetState(glass?ColorTargetState.DEFAULT:
                new ColorTargetState(new BlendFunction(BlendFactor.ONE,BlendFactor.ONE_MINUS_SRC_ALPHA,BlendFactor.ONE,BlendFactor.ONE_MINUS_SRC_ALPHA))).build());
    }
    private static GpuTexture hand,depth,background;
    private static GpuTextureView handView,depthView,backgroundView;
    private static GpuBuffer uniforms;
    private static Object world;
    private static boolean failed;
    private HandShaderRenderer(){}
    public static void initialize(){HandFlameRenderer.initialize();}
    public static void updateLifecycle(){
        var mc=Minecraft.getInstance();var settings=Shaders.current();
        if(world!=mc.level||settings==null||!settings.handsActive()||!mc.options.getCameraType().isFirstPerson()){
            close();failed=false;world=mc.level;
        }
    }
    public static void render(Runnable nativeDraw){
        updateLifecycle();var settings=Shaders.current();var mc=Minecraft.getInstance();
        if(settings==null||!settings.handsActive()||mc.level==null||failed){nativeDraw.run();return;}
        var oldColor=RenderSystem.outputColorTextureOverride;
        var oldDepth=RenderSystem.outputDepthTextureOverride;
        var target=oldColor!=null?oldColor:mc.gameRenderer.mainRenderTarget().getColorTextureView();
        var targetDepth=oldDepth!=null?oldDepth:mc.gameRenderer.mainRenderTarget().getDepthTextureView();
        if(target==null||targetDepth==null){nativeDraw.run();return;}
        var effect=settings.handMode().equals("Стекло")?GLASS:HOLOGRAM;
        boolean flame=settings.handMode().equals("Пламя");
        try{
            if(!flame&&!RenderSystem.getDevice().precompilePipeline(effect).isValid())throw new IllegalStateException("Hand shader compilation failed");
            prepare(target,targetDepth);
            if(flame)HandFlameRenderer.prepare(target.getWidth(0),target.getHeight(0));else HandFlameRenderer.close();
            var encoder=RenderSystem.getDevice().createCommandEncoder();
            if(effect==GLASS&&!flame)encoder.copyTextureToTexture(target.texture(),background,0,0,0,0,0,target.getWidth(0),target.getHeight(0));
            encoder.clearColorAndDepthTextures(hand,ShaderTextureUsage.transparentBlack(),depth,0.0);
        }catch(RuntimeException error){fail(error);nativeDraw.run();return;}
        RenderSystem.outputColorTextureOverride=handView;
        RenderSystem.outputDepthTextureOverride=depthView;
        try{nativeDraw.run();}
        finally{RenderSystem.outputColorTextureOverride=oldColor;RenderSystem.outputDepthTextureOverride=oldDepth;}
        try{
            if(flame){HandFlameRenderer.render(target,handView,settings.handColor());return;}
            try(var stack=MemoryStack.stackPush()){
                var data=stack.calloc(64);int color=settings.handColor();
                data.putFloat(0,(color>>16&255)/255f);data.putFloat(4,(color>>8&255)/255f);data.putFloat(8,(color&255)/255f);data.putFloat(12,.85f);
                data.putFloat(16,(System.nanoTime()%3_600_000_000_000L)/1e9f);data.putFloat(20,.7f);data.putFloat(24,.25f);data.putFloat(28,.6f);
                data.putFloat(32,1);data.putFloat(36,.7f);data.putFloat(40,.2f);data.putFloat(44,.45f);
                data.putFloat(48,target.getWidth(0));data.putFloat(52,target.getHeight(0));data.putFloat(56,1f/target.getWidth(0));data.putFloat(60,1f/target.getHeight(0));
                if(effect==GLASS){data.putFloat(12,.9f);data.putFloat(16,1.15f);data.putFloat(20,.25f);data.putFloat(24,.008f);data.putFloat(28,.6f);}
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniforms.slice(),data);
            }
            composite(target,effect);
        }catch(RuntimeException error){
            // The native submission is consumed: restore its captured pixels, never submit it twice.
            try{composite(target,PLAIN);}catch(RuntimeException fallback){error.addSuppressed(fallback);}
            fail(error);
        }
    }
    private static void prepare(GpuTextureView target,GpuTextureView targetDepth){
        if(hand!=null&&hand.getWidth(0)==target.getWidth(0)&&hand.getHeight(0)==target.getHeight(0)
            &&background.getFormat()==target.texture().getFormat()&&depth.getFormat()==targetDepth.texture().getFormat())return;
        close();world=Minecraft.getInstance().level;var device=RenderSystem.getDevice();
        if(!device.precompilePipeline(PLAIN).isValid())throw new IllegalStateException("Hand fallback compilation failed");
        int w=target.getWidth(0),h=target.getHeight(0);
        hand=device.createTexture("Vela hands",ShaderTextureUsage.CLEARED_TARGET,GpuFormat.RGBA8_UNORM,w,h,1,1);
        handView=device.createTextureView(hand);
        depth=device.createTexture("Vela hands depth",ShaderTextureUsage.CLEARED_TARGET,targetDepth.texture().getFormat(),w,h,1,1);
        depthView=device.createTextureView(depth);
        background=device.createTexture("Vela hands background",GpuTexture.USAGE_COPY_DST|GpuTexture.USAGE_TEXTURE_BINDING,target.texture().getFormat(),w,h,1,1);
        backgroundView=device.createTextureView(background);
        uniforms=device.createBuffer(()->"Vela hologram",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,64);
    }
    private static void composite(GpuTextureView target,RenderPipeline pipeline){
        try(var pass=RenderSystem.getDevice().createCommandEncoder().createRenderPass(()->"Vela hands composite",target,Optional.empty())){
            pass.setPipeline(pipeline);
            var sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            if(pipeline==GLASS){pass.bindTexture("SceneTex",backgroundView,sampler);pass.bindTexture("HandTex",handView,sampler);pass.setUniform("GlassConfig",uniforms);}
            else pass.bindTexture("SceneSampler",handView,sampler);
            if(pipeline==HOLOGRAM)pass.setUniform("HandsHologramData",uniforms);
            pass.draw(6,1,0,0);
        }
    }
    private static void fail(RuntimeException error){close();world=Minecraft.getInstance().level;failed=true;org.slf4j.LoggerFactory.getLogger("Vela").warn("Hand shaders disabled after GPU error",error);}
    public static void close(){
        HandFlameRenderer.close();
        if(handView!=null)handView.close();handView=null;if(depthView!=null)depthView.close();depthView=null;if(backgroundView!=null)backgroundView.close();backgroundView=null;
        if(hand!=null)hand.close();hand=null;if(depth!=null)depth.close();depth=null;if(background!=null)background.close();background=null;
        if(uniforms!=null)uniforms.close();uniforms=null;world=null;
    }
}
