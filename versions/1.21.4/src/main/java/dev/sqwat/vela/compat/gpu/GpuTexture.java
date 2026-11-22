package dev.sqwat.vela.compat.gpu;

import com.mojang.blaze3d.platform.TextureUtil;

/** 1.21.5's GPU texture on 1.21.4: an OpenGL texture name and its size. Borrowed ones (a render target's) are not freed. */
public final class GpuTexture implements AutoCloseable {
    int id,width,height;
    private final TextureFormat format;
    private final boolean owned;
    private boolean closed;
    GpuTexture(int id,TextureFormat format,int width,int height,boolean owned){this.id=id;this.format=format;this.width=width;this.height=height;this.owned=owned;}
    public int id(){return id;}
    public TextureFormat getFormat(){return format;}
    public int getWidth(int mip){return Math.max(1,width>>mip);}
    public int getHeight(int mip){return Math.max(1,height>>mip);}
    public boolean isClosed(){return closed;}
    @Override public void close(){if(closed)return;closed=true;if(owned)TextureUtil.releaseTextureId(id);}
}
