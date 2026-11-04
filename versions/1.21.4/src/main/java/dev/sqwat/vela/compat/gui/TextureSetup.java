package dev.sqwat.vela.compat.gui;

import dev.sqwat.vela.compat.gpu.Gpu;
import dev.sqwat.vela.compat.gpu.GpuTexture;
import org.jspecify.annotations.Nullable;

/** 1.21.6's texture binding of a GUI element, here just the texture for sampler 0 (or none). */
public record TextureSetup(@Nullable GpuTexture texture) {
    private static final TextureSetup NONE=new TextureSetup(null);
    public static TextureSetup noTexture(){return NONE;}
    public static TextureSetup singleTexture(GpuTexture texture){return new TextureSetup(texture);}
    public static TextureSetup of(net.minecraft.resources.ResourceLocation id){
        return new TextureSetup(Gpu.of(net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(id)));
    }
}
