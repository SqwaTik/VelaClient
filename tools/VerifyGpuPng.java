import dev.sqwat.vela.ui.render.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** Hidden-window OpenGL diagnostic for PNG sampling. Does not load or start Minecraft. */
public class VerifyGpuPng extends RenderPngPreview {
    record Quad(String key,float x,float y,float w,float h,int u,int v,int sw,int sh,int color){}
    final List<Quad> commands=new ArrayList<>();
    final Map<String,Integer> textures=new HashMap<>();
    VerifyGpuPng(Path assets,int width,int height,float density){super(assets,width,height,density);}
    @Override void quad(String key,float x,float y,float w,float h,int u,int v,int sw,int sh,int color){if(w>0&&h>0)commands.add(new Quad(key,x,y,w,h,u,v,sw,sh,color));}
    public static void main(String[] args)throws Exception {
        Path assets=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectories(out);
        if(!glfwInit())throw new IllegalStateException("GLFW init failed");
        glfwWindowHint(GLFW_VISIBLE,GLFW_FALSE);glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR,3);glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR,3);glfwWindowHint(GLFW_OPENGL_PROFILE,GLFW_OPENGL_CORE_PROFILE);
        long window=glfwCreateWindow(1800,1080,"Vela PNG verification",0,0);
        if(window==0){glfwTerminate();throw new IllegalStateException("Hidden graphics context unavailable");}
        try{
            glfwMakeContextCurrent(window);GL.createCapabilities();
            System.out.println("GPU: "+glGetString(GL_RENDERER));
            for(float scale:new float[]{1.125f,1.5f,1.875f,2.25f}){
                var p=new VerifyGpuPng(assets,Math.round(800*scale),Math.round(480*scale),scale);
                var layout=DashboardChrome.layout(800,480);
                DashboardChrome.draw(p,layout,0,"Рендеринг и визуальные эффекты","",false,new float[6],false,0xFF756CFF);
                DashboardChrome.empty(p,layout.content(),"Пока нет модулей","Добавленные функции появятся в этом разделе");
                p.render(out.resolve("gpu-dashboard-"+scale+".png"));
            }
            var a=new VerifyGpuPng(assets,1200,720,1.5f);var layout=AccountChrome.layout(800,480);
            AccountChrome.draw(a,layout,"SqwaT09",0,"",false,0,0,0xFF756CFF);DashboardChrome.empty(a,layout.list(),"Нет сохранённых аккаунтов","Добавьте аккаунт внизу");a.render(out.resolve("gpu-accounts.png"));
        }finally{glfwDestroyWindow(window);glfwTerminate();}
    }
    int shader(int type,String source){int id=glCreateShader(type);glShaderSource(id,source);glCompileShader(id);if(glGetShaderi(id,GL_COMPILE_STATUS)==0)throw new IllegalStateException(glGetShaderInfoLog(id));return id;}
    int texture(String key){return textures.computeIfAbsent(key,k->{
        BufferedImage image=image(k);int id=glGenTextures();glBindTexture(GL_TEXTURE_2D,id);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
        ByteBuffer bytes=MemoryUtil.memAlloc(image.getWidth()*image.getHeight()*4);
        try{for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int p=image.getRGB(x,y);bytes.put((byte)(p>>16)).put((byte)(p>>8)).put((byte)p).put((byte)(p>>>24));}bytes.flip();glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,image.getWidth(),image.getHeight(),0,GL_RGBA,GL_UNSIGNED_BYTE,bytes);}finally{MemoryUtil.memFree(bytes);}return id;
    });}
    void render(Path path)throws Exception {
        int vertex=shader(GL_VERTEX_SHADER,"#version 330\nlayout(location=0) in vec2 position;layout(location=1) in vec2 uv;uniform vec2 viewport;out vec2 texCoord;void main(){texCoord=uv;gl_Position=vec4(position.x/viewport.x*2.-1.,1.-position.y/viewport.y*2.,0.,1.);}");
        int fragment=shader(GL_FRAGMENT_SHADER,"#version 330\nuniform sampler2D Sampler0;uniform vec4 color;in vec2 texCoord;out vec4 fragColor;void main(){vec4 c=texture(Sampler0,texCoord)*color;if(c.a==0.)discard;fragColor=c;}");
        int program=glCreateProgram();glAttachShader(program,vertex);glAttachShader(program,fragment);glLinkProgram(program);if(glGetProgrami(program,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(program));
        int vao=glGenVertexArrays(),vbo=glGenBuffers();glBindVertexArray(vao);glBindBuffer(GL_ARRAY_BUFFER,vbo);glVertexAttribPointer(0,2,GL_FLOAT,false,16,0L);glVertexAttribPointer(1,2,GL_FLOAT,false,16,8L);glEnableVertexAttribArray(0);glEnableVertexAttribArray(1);
        glUseProgram(program);glUniform2f(glGetUniformLocation(program,"viewport"),output.getWidth(),output.getHeight());int colorLocation=glGetUniformLocation(program,"color");
        glViewport(0,0,output.getWidth(),output.getHeight());glDisable(GL_DEPTH_TEST);glEnable(GL_BLEND);glBlendFuncSeparate(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
        for(var command:commands)texture(command.key);
        long start=0;int frames=240;
        for(int frame=-20;frame<frames;frame++){
            if(frame==0)start=System.nanoTime();glClearColor(9/255f,12/255f,18/255f,1);glClear(GL_COLOR_BUFFER_BIT);
            for(var q:commands){
                BufferedImage image=image(q.key);float x=q.x*density,y=q.y*density,w=q.w*density,h=q.h*density,u=q.u/(float)image.getWidth(),v=q.v/(float)image.getHeight(),u2=(q.u+q.sw)/(float)image.getWidth(),v2=(q.v+q.sh)/(float)image.getHeight();
                glBindTexture(GL_TEXTURE_2D,texture(q.key));glUniform4f(colorLocation,(q.color>>16&255)/255f,(q.color>>8&255)/255f,(q.color&255)/255f,(q.color>>>24)/255f);
                glBufferData(GL_ARRAY_BUFFER,new float[]{x,y,u,v,x,y+h,u,v2,x+w,y+h,u2,v2,x,y,u,v,x+w,y+h,u2,v2,x+w,y,u2,v},GL_STREAM_DRAW);glDrawArrays(GL_TRIANGLES,0,6);
            }glFinish();
        }
        double ms=(System.nanoTime()-start)/1e6/frames;long gpuBytes=0;for(String key:textures.keySet())gpuBytes+=(long)image(key).getWidth()*image(key).getHeight()*4;
        ByteBuffer pixels=MemoryUtil.memAlloc(output.getWidth()*output.getHeight()*4);
        try{glReadPixels(0,0,output.getWidth(),output.getHeight(),GL_RGBA,GL_UNSIGNED_BYTE,pixels);for(int y=0;y<output.getHeight();y++)for(int x=0;x<output.getWidth();x++){int i=((output.getHeight()-1-y)*output.getWidth()+x)*4;int r=pixels.get(i)&255,g=pixels.get(i+1)&255,b=pixels.get(i+2)&255;output.setRGB(x,y,0xff000000|r<<16|g<<8|b);}ImageIO.write(output,"PNG",path.toFile());}finally{MemoryUtil.memFree(pixels);}
        System.out.printf(Locale.ROOT,"%s | %.3f ms/frame | %d quads | %.1f MiB PNG textures | GL error %d%n",path.getFileName(),ms,commands.size(),gpuBytes/1048576.0,glGetError());
        for(int texture:textures.values())glDeleteTextures(texture);textures.clear();glDeleteBuffers(vbo);glDeleteVertexArrays(vao);glDeleteProgram(program);glDeleteShader(vertex);glDeleteShader(fragment);graphics.dispose();
    }
}
