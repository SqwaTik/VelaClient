import java.nio.file.*;
import static org.lwjgl.util.shaderc.Shaderc.*;

/** Offline compiler check: creates no window or graphics context. */
public final class ValidateDissolveShaders {
    public static void main(String[] args)throws Exception{
        long compiler=shaderc_compiler_initialize(),options=shaderc_compile_options_initialize();
        if(compiler==0||options==0)throw new IllegalStateException("Shader compiler unavailable");
        try{
            shaderc_compile_options_set_auto_bind_uniforms(options,true);
            shaderc_compile_options_set_auto_map_locations(options,true);
            int[] targets=args.length>3&&args[3].equals("opengl")?new int[]{shaderc_target_env_opengl}:new int[]{shaderc_target_env_opengl,shaderc_target_env_vulkan};
            for(int target:targets){
                shaderc_compile_options_set_target_env(options,target,target==shaderc_target_env_opengl?shaderc_env_version_opengl_4_5:shaderc_env_version_vulkan_1_1);
                for(String ext:new String[]{"vsh","fsh"}){
                    String name=(args.length>1?args[1]:"dissolve")+"."+ext;
                    String source=Files.readString(Path.of(args[0],name)).replaceFirst("#version \\d+","#version 450");
                    if(source.contains("#moj_import <minecraft:globals.glsl>")){
                        if(args.length<3)throw new IllegalArgumentException("Minecraft JAR required for globals.glsl");
                        try(var jar=new java.util.zip.ZipFile(args[2])){
                            var entry=jar.getEntry("assets/minecraft/shaders/include/globals.glsl");
                            try(var input=jar.getInputStream(entry)){
                                String globals=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).replaceFirst("#version \\d+","");
                                source=source.replace("#moj_import <minecraft:globals.glsl>",globals);
                            }
                        }
                    }
                    long result=shaderc_compile_into_spv(compiler,source,ext.equals("vsh")?shaderc_glsl_vertex_shader:shaderc_glsl_fragment_shader,name,"main",options);
                    try{
                        if(shaderc_result_get_compilation_status(result)!=shaderc_compilation_status_success)throw new IllegalStateException(shaderc_result_get_error_message(result));
                        System.out.println(name+" target="+target+": passed");
                    }finally{shaderc_result_release(result);}
                }
            }
        }finally{shaderc_compile_options_release(options);shaderc_compiler_release(compiler);}
    }
}
