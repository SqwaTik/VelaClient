package dev.sqwat.vela;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.util.shaderc.Shaderc.*;

/** Offline GLSL validation: no game, window, driver context or world required. */
class ShaderFogCompilationTest {
    @Test void fogStagesCompile() throws Exception {
        long compiler=shaderc_compiler_initialize(),options=shaderc_compile_options_initialize();
        assertNotEquals(0,compiler);assertNotEquals(0,options);
        try{
            shaderc_compile_options_set_target_env(options,shaderc_target_env_opengl,shaderc_env_version_opengl_4_5);
            shaderc_compile_options_set_auto_bind_uniforms(options,true);
            shaderc_compile_options_set_auto_map_locations(options,true);
            for(String name:new String[]{"atmosphere_fog","wet_surfaces","wet_resolve","hands_hologram","hands_plain","hands_glass","hands_flame","hands_flame_trail","sky_aurora","sky_starfall","sky_blackhole","sky_bloom","sky_composite","sky_copy","hit_bubbles"})for(String extension:new String[]{"vsh","fsh"}){
                String vertex=name.equals("wet_resolve")?"wet_surfaces":name.startsWith("hands_")?"hands_hologram":name.startsWith("sky_")?"sky_aurora":name;
                String file="assets/velaclient/shaders/core/"+(extension.equals("vsh")?vertex:name)+"."+extension;
                try(var input=getClass().getClassLoader().getResourceAsStream(file)){
                    assertNotNull(input);String source=new String(input.readAllBytes(),StandardCharsets.UTF_8);
                    long result=shaderc_compile_into_spv(compiler,source,extension.equals("vsh")?shaderc_vertex_shader:shaderc_fragment_shader,file,"main",options);
                    assertNotEquals(0,result);
                    try{assertEquals(shaderc_compilation_status_success,shaderc_result_get_compilation_status(result),shaderc_result_get_error_message(result));}
                    finally{shaderc_result_release(result);}
                }
            }
            String example;
            try(var input=getClass().getClassLoader().getResourceAsStream("assets/velaclient/shaders/examples/aurora.glsl")) {
                assertNotNull(input);example=new String(input.readAllBytes(),StandardCharsets.UTF_8);
            }
            for(String body:new String[]{dev.sqwat.vela.render.shader.CustomSkySource.DEFAULT,example}) {
            long custom=shaderc_compile_into_spv(compiler,dev.sqwat.vela.render.shader.CustomSkySource.fragment(body),shaderc_fragment_shader,"user-sky.fsh","main",options);
            assertNotEquals(0,custom);
            try{assertEquals(shaderc_compilation_status_success,shaderc_result_get_compilation_status(custom),shaderc_result_get_error_message(custom));}
            finally{shaderc_result_release(custom);}
            }
        }finally{shaderc_compile_options_release(options);shaderc_compiler_release(compiler);}
    }
}
