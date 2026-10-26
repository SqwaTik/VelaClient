package dev.sqwat.vela.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Session-only zoom; never changes the user's FOV setting. */
public final class BuiltinZoom {
    private static float magnification=3, current=1;
    private static long previous;
    private static boolean active;
    private BuiltinZoom(){}
    public static boolean held(){
        var mc=Minecraft.getInstance();
        return mc.player!=null&&mc.gui.screen()==null&&mc.isWindowActive()
            &&GLFW.glfwGetKey(mc.getWindow().handle(),GLFW.GLFW_KEY_C)==GLFW.GLFW_PRESS;
    }
    public static void scroll(double amount){magnification=(float)Math.clamp(magnification*Math.pow(1.18,amount),1.25,16);}
    public static double sensitivityFactor(){return held()?Math.max(1d/16,Math.min(current,1/magnification)):1;}
    public static float factor(){
        long now=System.nanoTime();float dt=previous==0?0:Math.min(.05f,(now-previous)/1e9f);previous=now;
        boolean pressed=held();
        if(pressed&&!active)magnification=3;
        active=pressed;
        float target=pressed?1/magnification:1;
        current+=(target-current)*(float)(1-Math.exp(-10*dt));
        return current;
    }
}
