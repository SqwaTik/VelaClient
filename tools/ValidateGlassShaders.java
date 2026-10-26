import java.nio.file.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.util.shaderc.Shaderc.*;

/** Checks the actual shader sources without starting Minecraft. */
public class ValidateGlassShaders {
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);
        if(!glfwInit())throw new IllegalStateException("GLFW initialization failed");
        glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
        long window=glfwCreateWindow(64,64,"Glass shader validation",0,0);
        if(window==0)throw new IllegalStateException("Hidden GL context unavailable");
        try{
            glfwMakeContextCurrent(window);org.lwjgl.opengl.GL.createCapabilities();
            int[] shaders=new int[2];String[] suffix={"vsh","fsh"};
            long compiler=shaderc_compiler_initialize(),options=shaderc_compile_options_initialize();
            try{
                shaderc_compile_options_set_auto_bind_uniforms(options,true);
                shaderc_compile_options_set_auto_map_locations(options,true);
                shaderc_compile_options_set_target_env(options,shaderc_target_env_vulkan,shaderc_env_version_vulkan_1_1);
                for(int i=0;i<2;i++){
                    String name=args.length>2?args[i+1]:"glass."+suffix[i],source=Files.readString(root.resolve(name));
                    shaders[i]=glCreateShader(i==0?GL_VERTEX_SHADER:GL_FRAGMENT_SHADER);glShaderSource(shaders[i],source);glCompileShader(shaders[i]);
                    if(glGetShaderi(shaders[i],GL_COMPILE_STATUS)==0)throw new IllegalStateException(name+": "+glGetShaderInfoLog(shaders[i]));
                    long result=shaderc_compile_into_spv(compiler,source.replace("#version 330","#version 450").replace("gl_VertexID","gl_VertexIndex"),i==0?shaderc_glsl_vertex_shader:shaderc_glsl_fragment_shader,name,"main",options);
                    try{if(shaderc_result_get_compilation_status(result)!=shaderc_compilation_status_success)throw new IllegalStateException(shaderc_result_get_error_message(result));}
                    finally{shaderc_result_release(result);}
                    System.out.println(name+": OpenGL compilation and Vulkan SPIR-V syntax validation passed");
                }
                int program=glCreateProgram();for(int shader:shaders)glAttachShader(program,shader);glLinkProgram(program);
                if(glGetProgrami(program,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(program));
                glDeleteProgram(program);for(int shader:shaders)glDeleteShader(shader);
                System.out.println("OpenGL program link passed. Vulkan runtime bindings are not tested.");
            }finally{shaderc_compile_options_release(options);shaderc_compiler_release(compiler);}
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
}
