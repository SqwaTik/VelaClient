package dev.sqwat.vela.ui.render;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Radius-specific PNG masks with the same geometry as the offline renderer. */
final class PngChrome {
    private static final Identifier TEXTURE=Identifier.fromNamespaceAndPath("velaclient","textures/gui/chrome.png");
    private static final float ATLAS=1024f;
    /** Render-thread scratch: a nine-slice emits at most nine quads. Each element receives an exact-size copy. */
    private static final float[] SCRATCH=new float[9*8];
    private static int used;
    private static final ChromeAtlas.Sink SINK=(dx,dy,dw,dh,u,v,sw,sh)->{
        float[] q=SCRATCH;int i=used;
        q[i]=dx;q[i+1]=dy;q[i+2]=dx+dw;q[i+3]=dy+dh;
        q[i+4]=u/ATLAS;q[i+5]=v/ATLAS;q[i+6]=(u+sw)/ATLAS;q[i+7]=(v+sh)/ATLAS;
        used=i+8;
    };
    /** One GUI element per panel instead of one blit (pose copy, texture lookup, bounds) per slice. */
    static void draw(GuiGraphicsExtractor g,float x,float y,float width,float height,float radius,int color,boolean stroke){
        if((color>>>24)==0)return;
        used=0;
        ChromeAtlas.draw(x,y,width,height,radius,VelaDraw.pixelDensity(g),stroke,SINK);
        if(used==0)return;
        VelaDraw.quads(g,TEXTURE,java.util.Arrays.copyOf(SCRATCH,used),color);
    }
}
