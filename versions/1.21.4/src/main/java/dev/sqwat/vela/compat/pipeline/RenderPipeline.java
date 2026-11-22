package dev.sqwat.vela.compat.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * 1.21.5's render pipeline on 1.21.4, where render types carry the state themselves: a pipeline is a vanilla shader,
 * a vertex format and mode, blending, culling and depth state, turned into a render type by {@link #create}. Vela's own
 * shaders are written for the uniform blocks of newer versions, so a pipeline with one is not {@link #supported()}.
 */
public final class RenderPipeline {
    private final ResourceLocation location;
    private final Snippet base;
    private final @Nullable BlendFunction blend;
    private final boolean cull,depthWrite,custom;
    private final DepthTestFunction depthTest;
    private RenderPipeline(Builder b){
        location=b.location;base=b.base;blend=b.blend;cull=b.cull;depthWrite=b.depthWrite;depthTest=b.depthTest;custom=b.custom;
    }
    public ResourceLocation getLocation(){return location;}
    public boolean supported(){return !custom;}
    public VertexFormat format(){return base.format;}
    public VertexFormat.Mode mode(){return base.mode;}

    /** {@code RenderType.create(name, size, crumbling, sort, pipeline, state)}: the pipeline's shards over the given ones. */
    public static RenderType create(String name,int size,boolean crumbling,boolean sort,RenderPipeline pipeline,RenderType.CompositeState.CompositeStateBuilder state){
        pipeline.apply(state);
        try{return (RenderType)CREATE.invoke(name,pipeline.base.format,pipeline.base.mode,size,crumbling,sort,state.createCompositeState(false));}
        catch(Throwable error){throw new IllegalStateException("Cannot create render type "+name,error);}
    }
    public static RenderType create(String name,int size,RenderPipeline pipeline,RenderType.CompositeState.CompositeStateBuilder state){return create(name,size,false,false,pipeline,state);}

    private void apply(RenderType.CompositeState.CompositeStateBuilder state){
        state.setShaderState(new RenderStateShard.ShaderStateShard(base.shader));
        var blend=this.blend;
        state.setTransparencyState(blend==null?new RenderStateShard.TransparencyStateShard("vela_no_blend",RenderSystem::disableBlend,()->{})
            :new RenderStateShard.TransparencyStateShard("vela_blend",()->{RenderSystem.enableBlend();RenderSystem.blendFuncSeparate(blend.sourceColor(),blend.destColor(),blend.sourceAlpha(),blend.destAlpha());},
                ()->{RenderSystem.disableBlend();RenderSystem.defaultBlendFunc();}));
        state.setDepthTestState(new RenderStateShard.DepthTestStateShard(depthTest.name().toLowerCase(java.util.Locale.ROOT),depthTest.gl));
        state.setCullState(new RenderStateShard.CullStateShard(cull));
        // a pipeline with Vela's own shaders writes nothing: the vanilla shader under it would draw the wrong picture
        state.setWriteMaskState(new RenderStateShard.WriteMaskStateShard(!custom,depthWrite&&!custom));
    }

    /** The render type factory, found by its parameters: it is private and its name differs between the mappings. */
    private static final MethodHandle CREATE=factory();
    private static MethodHandle factory(){
        Class<?>[] wanted={String.class,VertexFormat.class,VertexFormat.Mode.class,int.class,boolean.class,boolean.class,RenderType.CompositeState.class};
        for(var method:RenderType.class.getDeclaredMethods()){
            if(Modifier.isStatic(method.getModifiers())&&RenderType.class.isAssignableFrom(method.getReturnType())&&Arrays.equals(method.getParameterTypes(),wanted)){
                try{method.setAccessible(true);return MethodHandles.lookup().unreflect(method);}
                catch(IllegalAccessException error){throw new IllegalStateException(error);}
            }
        }
        throw new IllegalStateException("RenderType has no composite factory");
    }

    public static Builder builder(Snippet... snippets){return new Builder(snippets);}

    /** A pipeline's shared part: vertex format and mode, vanilla shader, blending. */
    public static final class Snippet {
        final VertexFormat format;final VertexFormat.Mode mode;final ShaderProgram shader;final @Nullable BlendFunction blend;final boolean cull;
        Snippet(VertexFormat format,VertexFormat.Mode mode,ShaderProgram shader,@Nullable BlendFunction blend,boolean cull){
            this.format=format;this.mode=mode;this.shader=shader;this.blend=blend;this.cull=cull;
        }
    }

    public static final class Builder {
        private ResourceLocation location=ResourceLocation.withDefaultNamespace("pipeline/vela");
        private Snippet base=new Snippet(DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,CoreShaders.POSITION_COLOR,null,true);
        private @Nullable BlendFunction blend;
        private boolean cull=true,depthWrite=true,custom;
        private DepthTestFunction depthTest=DepthTestFunction.LEQUAL_DEPTH_TEST;
        private Builder(Snippet... snippets){for(var s:snippets){base=s;blend=s.blend;cull=s.cull;}}
        public Builder withLocation(String location){this.location=ResourceLocation.withDefaultNamespace(location);return this;}
        public Builder withLocation(ResourceLocation location){this.location=location;return this;}
        public Builder withVertexShader(ResourceLocation shader){custom=true;return this;}
        public Builder withVertexShader(String shader){custom=true;return this;}
        public Builder withFragmentShader(ResourceLocation shader){custom=true;return this;}
        public Builder withFragmentShader(String shader){custom=true;return this;}
        public Builder withBlend(BlendFunction blend){this.blend=blend;return this;}
        public Builder withoutBlend(){blend=null;return this;}
        public Builder withCull(boolean cull){this.cull=cull;return this;}
        public Builder withDepthWrite(boolean write){depthWrite=write;return this;}
        public Builder withDepthTestFunction(DepthTestFunction test){depthTest=test;return this;}
        public RenderPipeline build(){return new RenderPipeline(this);}
    }
}
