package dev.sqwat.vela.render;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.Identifier;
import java.util.Optional;

/** One reusable pipeline identity. Imported config code is never implicitly authorized. */
public final class CustomSkyProgram {
    private static final RenderPipeline PIPELINE=RenderPipeline.builder()
        .withLocation(Identifier.fromNamespaceAndPath("velaclient","pipeline/user_sky"))
        .withVertexShader(Identifier.fromNamespaceAndPath("velaclient","core/user_sky"))
        .withFragmentShader(Identifier.fromNamespaceAndPath("velaclient","core/user_sky"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("SkyParams",UniformType.UNIFORM_BUFFER).build())
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withCull(false).withDepthStencilState(Optional.empty()).withColorTargetState(ColorTargetState.DEFAULT).build();
    private static final String VERTEX="""
        #version 330
        out vec2 texCoord;
        void main(){
            vec2 p[6]=vec2[](vec2(-1,-1),vec2(1,-1),vec2(1,1),vec2(-1,-1),vec2(1,1),vec2(-1,1));
            gl_Position=vec4(p[gl_VertexID],0,1);texCoord=p[gl_VertexID]*0.5+0.5;
        }
        """;
    private static String accepted,fragment;
    private CustomSkyProgram(){}
    public static boolean accepted(String source){return source.equals(accepted);}
    public static RenderPipeline pipeline(){
        if(fragment==null)return null;
        return RenderSystem.getDevice().precompilePipeline(PIPELINE,(id,type)->type==ShaderType.VERTEX?VERTEX:fragment).isValid()?PIPELINE:null;
    }
    public static String apply(String source){
        String next;
        try{next=CustomSkySource.fragment(source);}catch(IllegalArgumentException error){return error.getMessage();}
        var device=RenderSystem.getDevice();
        // No per-edit pipeline identities accumulate in the device cache.
        device.clearPipelineCache();
        try{
            if(!device.precompilePipeline(PIPELINE,(id,type)->type==ShaderType.VERTEX?VERTEX:next).isValid()){
                String errors=String.join("\n",device.getLastDebugMessages());
                device.clearPipelineCache();
                return errors.isBlank()?"Ошибка GLSL. Подробности в latest.log":errors.substring(Math.max(0,errors.length()-3000));
            }
            accepted=source;fragment=next;return null;
        }catch(RuntimeException error){device.clearPipelineCache();return "Ошибка GLSL: "+error.getMessage();}
    }
}
