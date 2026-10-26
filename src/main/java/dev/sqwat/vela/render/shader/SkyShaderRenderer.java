package dev.sqwat.vela.render.shader;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.*;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.sqwat.vela.module.visual.Shaders;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import java.util.Optional;

/** Procedural sky replaces the native sky disc before terrain and translucent geometry. */
public final class SkyShaderRenderer {
    private static final RenderPipeline AURORA=pipeline("sky_aurora"),STARFALL=pipeline("sky_starfall"),BLACKHOLE=pipeline("sky_blackhole"),BLOOM=pipeline("sky_bloom"),COMPOSITE=pipeline("sky_composite"),COPY=pipeline("sky_copy");
    private static RenderPipeline pipeline(String name){
        var layout=BindGroupLayout.builder().withUniform(name.equals("sky_bloom")?"BloomParams":"SkyParams",UniformType.UNIFORM_BUFFER);
        if(name.equals("sky_blackhole")||name.equals("sky_aurora"))layout.withSampler("NoiseTex");
        if(name.equals("sky_bloom"))layout.withSampler("Source");
        if(name.equals("sky_copy"))layout.withSampler("Sky");
        if(name.equals("sky_composite")){layout.withSampler("Sky");for(int i=0;i<2;i++)layout.withSampler("Bloom"+i);}
        return RenderPipelines.register(RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/"+name))
        .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/sky_aurora"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/"+name))
        .withBindGroupLayout(layout.build())
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).withDepthStencilState(Optional.empty())
        .withColorTargetState(ColorTargetState.DEFAULT).build());}
    private static final Matrix4f projection=new Matrix4f(),inverse=new Matrix4f();
    private static final Matrix4f viewProjection=new Matrix4f();
    private static final GpuTexture[] textures=new GpuTexture[3];
    private static final GpuTextureView[] views=new GpuTextureView[3];
    private static GpuTexture noise;
    private static GpuTextureView noiseView;
    private static GpuSampler repeatSampler;
    // One parameter buffer per bloom level: rewriting a single buffer between two draws can stall the driver.
    private static final GpuBuffer[] bloomUniforms=new GpuBuffer[3];
    private static int bloomLevels;
    private static GpuBuffer uniforms;
    private static RenderPipeline compiled;
    private static Object world;
    private static boolean ready,rendered,failed;
    private SkyShaderRenderer(){}
    public static void initialize(){}
    public static void captureProjection(Matrix4f actual){projection.set(actual);}
    public static void beginFrame(CameraRenderState camera){
        rendered=false;ready=false;var mc=Minecraft.getInstance();var settings=Shaders.current();
        if(world!=mc.level||settings==null||!settings.skyActive()){close();failed=false;world=mc.level;}
        if(failed||settings==null||!settings.skyActive()||mc.level==null||camera.fogType!=FogType.NONE)return;
        viewProjection.set(projection).mul(camera.viewRotationMatrix);inverse.set(viewProjection).invert();ready=true;
    }
    public static boolean rendered(){return rendered;}
    public static boolean render(RenderTarget target){
        if(!ready)return false;var settings=Shaders.current();if(settings==null)return false;
        var pipeline=switch(settings.skyMode()){case "Звездопад"->STARFALL;case "Чёрная дыра"->BLACKHOLE;default->AURORA;};
        try{
            if(settings.skyMode().equals("GLSL")&&CustomSkyProgram.accepted(settings.skyCode())){
                var custom=CustomSkyProgram.pipeline();if(custom!=null)pipeline=custom;
            }
            var device=RenderSystem.getDevice();
            boolean custom=pipeline!=AURORA&&pipeline!=STARFALL&&pipeline!=BLACKHOLE;
            var composite=custom?COPY:COMPOSITE;
            if(compiled!=pipeline){if(!device.precompilePipeline(pipeline).isValid()||(!custom&&!device.precompilePipeline(BLOOM).isValid())||!device.precompilePipeline(composite).isValid())throw new IllegalStateException("Sky shader compilation failed");compiled=pipeline;}
            ensureTargets(target,resolutionDivisor(target.getColorTexture().getWidth(0),target.getColorTexture().getHeight(0)),custom?0:2);
            boolean usesNoise=pipeline==BLACKHOLE||pipeline==AURORA;
            if(usesNoise)ensureNoise();
            if(uniforms==null)uniforms=device.createBuffer(()->"Vela sky parameters",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,208);
            try(var stack=MemoryStack.stackPush()){
                var data=stack.calloc(208);inverse.get(0,data);
                data.putFloat(64,(System.nanoTime()%3_600_000_000_000L)/1e9f);
                data.putFloat(68,target.getColorTexture().getWidth(0));data.putFloat(72,target.getColorTexture().getHeight(0));data.putFloat(76,settings.skyBrightness());
                int color=settings.skyColor();data.putFloat(80,(color>>16&255)/255f);data.putFloat(84,(color>>8&255)/255f);data.putFloat(88,(color&255)/255f);
                data.putFloat(92,0);data.putFloat(196,device.getDeviceInfo().isZZeroToOne()?1:0);
                data.putFloat(108,0);data.putFloat(112,0);data.putFloat(116,0);
                device.createCommandEncoder().writeToBuffer(uniforms.slice(),data);
            }
            var sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            try(var pass=device.createCommandEncoder().createRenderPass(()->"Vela procedural sky",views[0],Optional.empty())){
                pass.setPipeline(pipeline);pass.setUniform("SkyParams",uniforms);
                if(usesNoise)pass.bindTexture("NoiseTex",noiseView,repeatSampler);
                pass.draw(6,1,0,0);
            }
            var source=views[0];
            for(int i=1;i<=bloomLevels;i++){
                try(var stack=MemoryStack.stackPush()){
                    var data=stack.calloc(16);data.putFloat(0,1f/source.getWidth(0));data.putFloat(4,1f/source.getHeight(0));data.putFloat(8,i==1?.65f:0);
                    device.createCommandEncoder().writeToBuffer(bloomUniforms[i].slice(),data);
                }
                try(var pass=device.createCommandEncoder().createRenderPass(()->"Vela sky bloom",views[i],Optional.empty())){
                    pass.setPipeline(BLOOM);pass.setUniform("BloomParams",bloomUniforms[i]);pass.bindTexture("Source",source,sampler);pass.draw(6,1,0,0);
                }
                source=views[i];
            }
            try(var pass=device.createCommandEncoder().createRenderPass(()->"Vela sky composite",target.getColorTextureView(),Optional.empty())){
                pass.setPipeline(composite);pass.setUniform("SkyParams",uniforms);pass.bindTexture("Sky",views[0],sampler);
                for(int i=0;i<bloomLevels;i++)pass.bindTexture("Bloom"+i,views[1+i],sampler);
                pass.draw(6,1,0,0);
            }
            rendered=true;return true;
        }catch(RuntimeException error){close();world=Minecraft.getInstance().level;failed=true;org.slf4j.LoggerFactory.getLogger("Vela").warn("Sky shader disabled after GPU error",error);return false;}
    }
    /** Bounds fragment work to 960x540, while retaining the screen's aspect ratio. */
    public static int resolutionDivisor(int width,int height){
        return Math.max(2,Math.max((width-1)/960+1,(height-1)/540+1));
    }
    private static void ensureTargets(RenderTarget target,int scale,int levels){
        int w=Math.max(1,target.getColorTexture().getWidth(0)/scale),h=Math.max(1,target.getColorTexture().getHeight(0)/scale);
        if(textures[0]!=null&&textures[0].getWidth(0)==w&&textures[0].getHeight(0)==h&&bloomLevels==levels)return;
        closeTargets();bloomLevels=levels;var device=RenderSystem.getDevice();
        for(int i=0;i<=levels;i++){
            if(i>=1){w=Math.max(1,w/2);h=Math.max(1,h/2);}
            textures[i]=device.createTexture("Vela sky layer "+i,ShaderTextureUsage.CLEARED_TARGET,GpuFormat.RGBA8_UNORM,w,h,1,1);
            views[i]=device.createTextureView(textures[i]);
            device.createCommandEncoder().clearColorTexture(textures[i],ShaderTextureUsage.transparentBlack());
        }
        for(int i=1;i<=levels;i++)bloomUniforms[i]=device.createBuffer(()->"Vela sky bloom",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,16);
    }
    private static void ensureNoise(){
        if(noise!=null)return;var device=RenderSystem.getDevice();
        var random=new java.util.Random(1592639215L);int[] red=new int[65536],blue=new int[65536];
        for(int i=0;i<red.length;i++){red[i]=random.nextInt(256);blue[i]=random.nextInt(256);}
        noise=device.createTexture("Vela black hole noise",GpuTexture.USAGE_COPY_DST|GpuTexture.USAGE_TEXTURE_BINDING,GpuFormat.RGBA8_UNORM,256,256,1,1);
        noiseView=device.createTextureView(noise);
        try(var image=new NativeImage(256,256,false)){
            for(int y=0;y<256;y++)for(int x=0;x<256;x++)image.setPixelABGR(x,y,0xff000000|blue[y*256+x]<<16|red[Math.floorMod(y-17,256)*256+Math.floorMod(x-37,256)]<<8|red[y*256+x]);
            device.createCommandEncoder().writeToTexture(noise,image);
        }
        repeatSampler=device.createSampler(AddressMode.REPEAT,AddressMode.REPEAT,FilterMode.LINEAR,FilterMode.LINEAR,1,java.util.OptionalDouble.empty());
    }
    private static void closeTargets(){
        for(int i=0;i<textures.length;i++){if(views[i]!=null)views[i].close();views[i]=null;if(textures[i]!=null)textures[i].close();textures[i]=null;}
        for(int i=0;i<bloomUniforms.length;i++){if(bloomUniforms[i]!=null)bloomUniforms[i].close();bloomUniforms[i]=null;}
    }
    public static void close(){closeTargets();if(noiseView!=null)noiseView.close();noiseView=null;if(noise!=null)noise.close();noise=null;if(repeatSampler!=null)repeatSampler.close();repeatSampler=null;if(uniforms!=null)uniforms.close();uniforms=null;compiled=null;world=null;ready=false;rendered=false;}
}
