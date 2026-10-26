package dev.sqwat.vela.ui.render;

/** Small metric symbols drawn on the same pixel grid as the HUD. */
public final class MetricIcon {
    private MetricIcon(){}
    public static void draw(UiCanvas g,String metric,float x,float y,int color){
        if(metric.endsWith(" FPS")){
            g.border(x,y,9,6,1,color);g.fill(x+4,y+6,1,2,color);g.fill(x+2,y+8,5,1,color);
        }else if(metric.endsWith(" TPS")){
            g.border(x,y,9,9,4.5f,color);g.fill(x+4,y+2,1,3,color);g.fill(x+4,y+4,3,1,color);
        }else if(metric.endsWith(" BPS")){
            // Two forward chevrons, 1 px strokes on the HUD pixel grid.
            for(int k=0;k<2;k++){float ox=x+1+k*4;
                g.fill(ox,y+1,1,1,color);g.fill(ox+1,y+2,1,1,color);g.fill(ox+2,y+3,1,1,color);g.fill(ox+3,y+4,1,1,color);
                g.fill(ox+2,y+5,1,1,color);g.fill(ox+1,y+6,1,1,color);g.fill(ox,y+7,1,1,color);}
        }else if(metric.endsWith(" online")){
            g.round(x+3,y,3,3,1.5f,color);g.round(x+1,y+4,7,5,2.5f,color);
        }else if(metric.length()==5&&metric.charAt(2)==':'){
            g.border(x,y,9,9,2,color);g.fill(x+4,y+2,1,3,color);g.fill(x+4,y+4,3,1,color);
        }else if(metric.endsWith(" ms")){
            // Connection: signal bars (TPS keeps the clock face).
            g.round(x,y+6,2,3,.5f,color);g.round(x+3,y+3,2,6,.5f,color);g.round(x+6,y,2,9,.5f,color);
        }else{
            g.round(x,y+6,2,3,.5f,color);g.round(x+3,y+3,2,6,.5f,color);g.round(x+6,y,2,9,.5f,color);
        }
    }
    public static String value(String metric){return metric;}
}
