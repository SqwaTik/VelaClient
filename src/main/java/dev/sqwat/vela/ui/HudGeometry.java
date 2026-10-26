package dev.sqwat.vela.ui;

import dev.sqwat.vela.ui.render.Rect;

public final class HudGeometry {
    public static final float CONTEXT_SCALE=1f;
    public static final int CONTEXT_WIDTH=108;
    private HudGeometry() {}
    /** Saved notification position is the top-left of the 128x24 editor placeholder. */
    public static int notificationOffset(int actualWidth) { return Math.round((128-actualWidth)/2f); }
    public static int nearWidth(float labelWidth) { return Math.clamp((int)Math.ceil(labelWidth)+76,112,200); }
    public static int toggleX(int x,int width,boolean expandable) { return x+width-(expandable?49:29); }
    public static Rect toggleHit(int x,int y) { return new Rect(x-2,y-2,23,15); }
    public static Rect context(Rect hud,int width,int height) {
        return context(hud,width,height,CONTEXT_WIDTH,58);
    }
    public static Rect context(Rect hud,int width,int height,int requestedWidth,int requestedHeight) {
        int w=Math.min(requestedWidth,Math.max(1,width-4)),h=Math.min(requestedHeight,Math.max(1,height-4));
        int x=hud.x()+hud.width()+4;
        if(x+w>width-2)x=hud.x()-w-4;
        return new Rect(Math.max(2,Math.min(width-w-2,x)),Math.max(2,Math.min(height-h-2,hud.y())),w,h);
    }
}
