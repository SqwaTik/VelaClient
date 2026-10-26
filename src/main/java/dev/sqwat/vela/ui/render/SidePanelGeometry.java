package dev.sqwat.vela.ui.render;

/** Keeps the drawer outside the window when space allows, inside the viewport otherwise. */
public final class SidePanelGeometry {
    private SidePanelGeometry() {}
    public static Rect place(int viewportWidth,int viewportHeight,Rect window,int panelHeight,float reveal){
        int w=Math.min(180,Math.max(1,viewportWidth-16));
        int h=Math.min(panelHeight,Math.max(1,viewportHeight-16));
        int right=Math.max(8,viewportWidth-w-8);
        int x=Math.min(right,window.x()+window.width()+8+Math.round(10*(1-Math.max(0,Math.min(1,reveal)))));
        int y=Math.max(8,Math.min(window.y()+38,viewportHeight-h-8));
        return new Rect(Math.max(8,x),y,w,h);
    }
}
