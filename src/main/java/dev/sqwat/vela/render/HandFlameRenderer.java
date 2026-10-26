package dev.sqwat.vela.render;
import com.mojang.blaze3d.*;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryStack;
import java.util.Optional;

/** Bounded one-third-resolution history, driven by elapsed time rather than frame count. */
public final class HandFlameRenderer {
    private static final RenderPipeline TRAIL=pipeline("hands_flame_trail",true),COMPOSITE=pipeline("hands_flame",false);
    private static RenderPipeline pipeline(String name,boolean trail){
        var layout=BindGroupLayout.builder().withSampler("AfterSampler").withSampler(trail?"PrevTrailSampler":"TrailSampler").withUniform("HandsFlameData",UniformType.UNIFORM_BUFFER);
        return RenderPipelines.register(RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/"+name))
            .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/hands_hologram")).withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/"+name))
            .withBindGroupLayout(layout.build()).withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false)
            .withDepthStencilState(Optional.empty()).withColorTargetState(trail?ColorTargetState.DEFAULT:
                new ColorTargetState(new BlendFunction(BlendFactor.ONE,BlendFactor.ONE_MINUS_SRC_ALPHA,BlendFactor.ONE,BlendFactor.ONE_MINUS_SRC_ALPHA))).build());
    }
    private static final GpuTexture[] textures=new GpuTexture[2];
    private static final GpuTextureView[] views=new GpuTextureView[2];
    private static GpuBuffer uniforms;
    private static long previousTime;
    private static int write;
    private HandFlameRenderer(){}
    public static void initialize(){}
    public static void prepare(int width,int height){
        int w=Math.max(1,width/3),h=Math.max(1,height/3);
        if(textures[0]!=null&&textures[0].getWidth(0)==w&&textures[0].getHeight(0)==h)return;
        close();var device=RenderSystem.getDevice();
        if(!device.precompilePipeline(TRAIL).isValid()||!device.precompilePipeline(COMPOSITE).isValid())throw new IllegalStateException("Flame shader compilation failed");
        for(int i=0;i<2;i++){
            textures[i]=device.createTexture("Vela hand flame "+i,ShaderTextureUsage.CLEARED_TARGET,GpuFormat.RGBA8_UNORM,w,h,1,1);
            views[i]=device.createTextureView(textures[i]);device.createCommandEncoder().clearColorTexture(textures[i],ShaderTextureUsage.transparentBlack());
        }
        uniforms=device.createBuffer(()->"Vela flame parameters",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,144);
    }
    public static void render(GpuTextureView target,GpuTextureView hand,int color){
        var device=RenderSystem.getDevice();long now=System.nanoTime();float dt=previousTime==0?1f/60:Math.clamp((now-previousTime)/1e9f,.0005f,.05f);previousTime=now;
        try(var stack=MemoryStack.stackPush()){
            var data=stack.calloc(144);
            data.putFloat(0,(color>>16&255)/255f);data.putFloat(4,(color>>8&255)/255f);data.putFloat(8,(color&255)/255f);data.putFloat(12,dt);
            data.putFloat(16,.85f);data.putFloat(20,.85f);data.putFloat(24,.65f);data.putFloat(28,1.2f);
            data.putFloat(32,1);data.putFloat(36,(now%3_600_000_000_000L)/1e9f);data.putFloat(40,1);data.putFloat(44,1);
            data.putFloat(48,target.getWidth(0));data.putFloat(52,target.getHeight(0));data.putFloat(56,1f/target.getWidth(0));data.putFloat(60,1f/target.getHeight(0));
            device.createCommandEncoder().writeToBuffer(uniforms.slice(),data);
        }
        var sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        try(var pass=device.createCommandEncoder().createRenderPass(()->"Vela flame history",views[write],Optional.empty())){
            pass.setPipeline(TRAIL);pass.setUniform("HandsFlameData",uniforms);pass.bindTexture("AfterSampler",hand,sampler);pass.bindTexture("PrevTrailSampler",views[1-write],sampler);pass.draw(6,1,0,0);
        }
        try(var pass=device.createCommandEncoder().createRenderPass(()->"Vela flame composite",target,Optional.empty())){
            pass.setPipeline(COMPOSITE);pass.setUniform("HandsFlameData",uniforms);pass.bindTexture("AfterSampler",hand,sampler);pass.bindTexture("TrailSampler",views[write],sampler);pass.draw(6,1,0,0);
        }
        write=1-write;
    }
    public static void close(){
        for(int i=0;i<2;i++){if(views[i]!=null)views[i].close();views[i]=null;if(textures[i]!=null)textures[i].close();textures[i]=null;}
        if(uniforms!=null)uniforms.close();uniforms=null;previousTime=0;write=0;
    }
}
