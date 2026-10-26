package dev.sqwat.vela.ui.render;

public final class CrosshairChrome {
    private CrosshairChrome(){}
    public static void draw(UiCanvas c,float x,float y,float length,float width,float opening,boolean dot,int color){
            c.fill(x-opening-length,y-width/2,length,width,color);
            c.fill(x+opening,y-width/2,length,width,color);
            c.fill(x-width/2,y-opening-length,width,length,color);
            c.fill(x-width/2,y+opening,width,length,color);
        if(dot)c.round(x-width/2,y-width/2,width,width,width/2,color);
    }
}
