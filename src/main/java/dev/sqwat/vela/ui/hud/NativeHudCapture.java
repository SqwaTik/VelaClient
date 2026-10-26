package dev.sqwat.vela.ui.hud;

import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.ui.render.Rect;

/** Measures actual vanilla HUD extraction. No placeholder entities or bars. */
public final class NativeHudCapture {
    private static String active;
    private static Rect bounds;
    private static int screenWidth;
    private NativeHudCapture() {}
    public static void begin(String id,int width){active=id;bounds=null;screenWidth=width;}
    public static boolean active(){return active!=null;}
    public static void rectangle(int x0,int y0,int x1,int y1){
        if(active==null)return;
        int x=Math.min(x0,x1),y=Math.min(y0,y1),right=Math.max(x0,x1),bottom=Math.max(y0,y1);
        if(bounds!=null){x=Math.min(x,bounds.x());y=Math.min(y,bounds.y());right=Math.max(right,bounds.x()+bounds.width());bottom=Math.max(bottom,bounds.y()+bounds.height());}
        bounds=new Rect(x,y,right-x,bottom-y);
    }
    public static void text(int x,int y,int width,int height){
        if(active==null)return;
        rectangle(x,y,x+width,y+height);
        if(active.equals("bossbar"))rectangle(screenWidth/2-91,y+9,screenWidth/2+91,y+14);
    }
    public static void finish(){
        if(active!=null&&VelaClient.services()!=null)VelaClient.services().hudLayout().recordNative(active,bounds);
        active=null;bounds=null;
    }
}
