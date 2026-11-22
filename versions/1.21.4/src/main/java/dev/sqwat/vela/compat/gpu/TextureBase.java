package dev.sqwat.vela.compat.gpu;

import net.minecraft.client.renderer.texture.AbstractTexture;

/** A texture that owns a {@link GpuTexture}, as 1.21.5's AbstractTexture does with its {@code texture} field. */
public abstract class TextureBase extends AbstractTexture {
    protected GpuTexture texture;
    public GpuTexture getTexture(){return texture;}
    @Override public int getId(){return texture!=null?texture.id():super.getId();}
    @Override public void releaseId(){if(texture!=null){texture.close();texture=null;}else super.releaseId();}
}
