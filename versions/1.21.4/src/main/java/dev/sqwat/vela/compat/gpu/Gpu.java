package dev.sqwat.vela.compat.gpu;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jspecify.annotations.Nullable;
import java.util.Map;
import java.util.WeakHashMap;

/** 1.21.5's GPU objects for 1.21.4 code: the device, a render target's colour texture and a texture's GPU side. */
public final class Gpu {
    private static final GpuDevice DEVICE=new GpuDevice();
    private static final Map<Object,GpuTexture> BORROWED=new WeakHashMap<>();
    private Gpu(){}
    public static GpuDevice device(){return DEVICE;}
    /** {@code target.getColorTexture()}. */
    public static @Nullable GpuTexture color(RenderTarget target){
        int id=target.getColorTextureId();if(id<=0)return null;
        return borrowed(target,id,target.width,target.height);
    }
    /** {@code texture.getTexture()}. */
    public static @Nullable GpuTexture of(@Nullable AbstractTexture texture){
        if(texture==null)return null;
        if(texture instanceof TextureBase base)return base.getTexture();
        return borrowed(texture,texture.getId(),0,0);
    }
    private static GpuTexture borrowed(Object owner,int id,int width,int height){
        var texture=BORROWED.get(owner);
        if(texture==null||texture.isClosed())BORROWED.put(owner,texture=new GpuTexture(id,TextureFormat.RGBA8,width,height,false));
        texture.id=id;texture.width=width;texture.height=height;return texture;
    }
}
