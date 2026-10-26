package dev.sqwat.vela.ui.render;

/** Small metric symbols drawn on the same pixel grid as the HUD. */
public final class MetricIcon {
    private MetricIcon(){}
    public static void draw(UiCanvas g,String metric,float x,float y,int color){
        if(metric.endsWith(" FPS")){
            g.border(x,y,9,6,1,color);g.fill(x+4,y+6,1,2,color);g.fill(x+2,y+8,5,1,color);
        }else if(metric.endsWith(" TPS")){
            g.border(x,y,9,9,4.5f,color);g.fill(x+4,y+2,1,3,color);g.fill(x+4,y+4,3,1,color);
        }else if(metric.endsWith(" ms")){
            g.border(x,y,9,9,4.5f,color);g.fill(x+4,y+1,1,4,color);g.fill(x+4,y+4,3,1,color);
        }else{
            g.round(x,y+6,2,3,.5f,color);g.round(x+3,y+3,2,6,.5f,color);g.round(x+6,y,2,9,.5f,color);
        }
    }
    public static String value(String metric){return metric;}
}
