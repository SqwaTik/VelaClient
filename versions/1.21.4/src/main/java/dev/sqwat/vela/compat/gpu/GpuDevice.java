package dev.sqwat.vela.compat.gpu;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Supplier;

/** {@code RenderSystem.getDevice()} on 1.21.4: makes plain OpenGL textures. */
public final class GpuDevice {
    private final CommandEncoder encoder=new CommandEncoder();
    GpuDevice(){}
    public GpuTexture createTexture(Supplier<String> label,TextureFormat format,int width,int height,int mips){return createTexture(label.get(),format,width,height,mips);}
    public GpuTexture createTexture(String label,TextureFormat format,int width,int height,int mips){
        RenderSystem.assertOnRenderThreadOrInit();
        int id=TextureUtil.generateTextureId();
        TextureUtil.prepareImage(id,Math.max(0,mips-1),width,height);
        return new GpuTexture(id,format,width,height,true);
    }
    public CommandEncoder createCommandEncoder(){return encoder;}
}
