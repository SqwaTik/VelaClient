package dev.sqwat.vela.compat.gpu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** 1.21.5's command encoder on 1.21.4: texture copies through a read framebuffer and image uploads. */
public final class CommandEncoder {
    private int framebuffer;
    CommandEncoder(){}
    public void copyTextureToTexture(GpuTexture source,GpuTexture target,int mip,int toX,int toY,int fromX,int fromY,int width,int height){
        if(framebuffer==0)framebuffer=GL30.glGenFramebuffers();
        int previous=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,source.id(),0);
        try{
            GlStateManager._bindTexture(target.id());
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D,mip,toX,toY,fromX,fromY,width,height);
        }finally{
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,0,0);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,previous);
        }
    }
    public void writeToTexture(GpuTexture target,NativeImage image){
        GlStateManager._bindTexture(target.id());
        image.upload(0,0,0,false);
    }
}
