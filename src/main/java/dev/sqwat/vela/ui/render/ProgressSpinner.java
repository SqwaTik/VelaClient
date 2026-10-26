package dev.sqwat.vela.ui.render;
/** Shared rotating highlight on a quiet circular track. */
public final class ProgressSpinner {
    private ProgressSpinner(){}
    public static void draw(UiCanvas c,float x,float y,float radius,long nanos){
        draw(c,x,y,radius,nanos,1);
    }
    public static void draw(UiCanvas c,float x,float y,float radius,long nanos,float opacity){
        if(opacity<=0)return;
        float phase=(nanos%1_100_000_000L)/1_100_000_000f;
        for(int i=0;i<96;i++){
            double angle=i*Math.PI*2/96;
            float tail=(i/96f-phase+1)%1;
            int color=VelaTheme.mix(VelaTheme.BORDER,0xFFF6F7FF,tail<.28f?1-tail/.28f:0);
            color=(Math.round(255*Math.clamp(opacity,0,1))<<24)|(color&0xFFFFFF);
            c.round(x+(float)Math.cos(angle)*radius-1,y+(float)Math.sin(angle)*radius-1,2,2,1,color);
        }
    }
}
