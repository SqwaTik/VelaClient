package dev.sqwat.vela.render.perf;

/** Last completed texture-buffer attachment within a render pass. */
public final class TextureBufferBinding {
    private int texture,format,buffer;
    private boolean valid;
    public boolean matches(int texture,int format,int buffer){return valid&&texture>0&&buffer>0&&this.texture==texture&&this.format==format&&this.buffer==buffer;}
    public void record(int texture,int format,int buffer){this.texture=texture;this.format=format;this.buffer=buffer;valid=texture>0&&buffer>0;}
    public void clear(){valid=false;}
}
