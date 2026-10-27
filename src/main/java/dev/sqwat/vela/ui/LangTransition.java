package dev.sqwat.vela.ui;

import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Language switch transition: the interface dims behind a small loader, the language changes while it is covered, and
 * the new text fades in. Drawn on top of every Vela screen (VelaScreen) for well under a second.
 */
public final class LangTransition {
    private static final long IN=180_000_000L,HOLD=360_000_000L,OUT=260_000_000L;
    private static long start=-1;
    private static Runnable commit;
    private static String label="";
    private LangTransition(){}

    /** Starts the transition; {@code apply} switches the language once the overlay covers the screen. */
    public static void begin(Lang.Language target,Runnable apply){
        if(target==Lang.current()&&start<0)return;
        start=System.nanoTime();commit=apply;label=target==Lang.Language.EN?"English":"Русский";
    }
    public static boolean running(){return start>=0;}

    public static void draw(GuiGraphicsExtractor g,int width,int height){
        if(start<0)return;
        long t=System.nanoTime()-start;
        if(t>=IN&&commit!=null){commit.run();commit=null;}
        if(t>=IN+HOLD+OUT){start=-1;return;}
        float cover=t<IN?ease(t/(float)IN):t<IN+HOLD?1:1-ease((t-IN-HOLD)/(float)OUT);
        g.nextStratum();
        g.fill(0,0,width,height,(Math.round(cover*0xE6)<<24)|(VelaTheme.WINDOW&0xFFFFFF));
        int a=Math.round(cover*255);if(a<4)return;
        float cx=width/2f,cy=height/2f;
        // Loader: an accent arc of dots that runs around a faint ring.
        double spin=(System.nanoTime()/1e9)*Math.PI*2*1.1;
        for(int i=0;i<24;i++){
            double angle=spin+i*Math.PI*2/24;
            float head=i/23f;int alpha=Math.round(a*(.12f+.88f*head*head));
            int px=Math.round(cx+(float)Math.cos(angle)*11),py=Math.round(cy-8+(float)Math.sin(angle)*11);
            int color=i>15?VelaTheme.PRIMARY:VelaTheme.TEXT_MUTED;
            g.fill(px-1,py-1,px+1,py+1,(alpha<<24)|(color&0xFFFFFF));
        }
        VelaDraw.centeredText(g,label,cx,cy+10,(a<<24)|(VelaTheme.TEXT&0xFFFFFF),VelaDraw.Weight.MEDIUM,9f);
    }
    private static float ease(float x){x=Math.clamp(x,0,1);return x*x*(3-2*x);}
}
