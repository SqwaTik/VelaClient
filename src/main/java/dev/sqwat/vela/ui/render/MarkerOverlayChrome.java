package dev.sqwat.vela.ui.render;

import dev.sqwat.vela.marker.MarkerLabelGeometry;

public final class MarkerOverlayChrome {
    private MarkerOverlayChrome(){}
    public static void label(UiCanvas c,String name,String distance,String icon,int color){
        int width=MarkerLabelGeometry.titleWidth(c.textWidth(name,8,UiCanvas.Weight.MEDIUM));
        float left=-width/2f;
        c.round(left,-37,width,22,6,0xB810141C);c.border(left,-37,width,22,6,0x70576578);
        c.image(icon,left+5,-32,10,color);c.text(name,left+17,-30,8,VelaTheme.TEXT,UiCanvas.Weight.MEDIUM);
        int dw=MarkerLabelGeometry.distanceWidth(c.textWidth(distance,8,UiCanvas.Weight.REGULAR));
        c.round(-dw/2f,-12,dw,15,2,0xB810141C);c.border(-dw/2f,-12,dw,15,2,0x70576578);
        c.centered(distance,0,-8,8,0xFFFFFFFF,UiCanvas.Weight.REGULAR);c.round(-2.5f,10,5,5,3,VelaTheme.PRIMARY);
    }
    public static Rect notice(UiCanvas c,int x,int y,int width,String name,String icon,int color){
        c.round(x,y,width,30,7,VelaTheme.SURFACE);c.border(x,y,width,30,7,VelaTheme.BORDER);
        c.image(icon,x+8,y+9,12,color);
        c.text(name,x+26,y+11,8,VelaTheme.TEXT,UiCanvas.Weight.REGULAR);
        return new Rect(x+width-49,y+5,43,20);
    }
}
