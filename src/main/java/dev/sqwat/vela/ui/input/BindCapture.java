package dev.sqwat.vela.ui.input;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Key capture for every bind button. Pressing a modifier does not finish the capture: it waits for the main key, so
 * "Ctrl, then F" binds Ctrl+F the way people press it. Releasing a modifier with no other key binds that modifier
 * alone. Only one capture runs at a time, so the pending modifier is shared.
 */
public final class BindCapture {
    /** Returned while the capture still waits for the main key. */
    public static final int PENDING=-2;
    private static int pendingModifier=-1;
    private BindCapture(){}

    /** A key press during capture: {@link #PENDING} for a modifier, otherwise the bind including held modifiers. */
    public static int press(int key,int glfwModifiers){
        if(BindInput.isModifier(key)){pendingModifier=key;return PENDING;}
        pendingModifier=-1;
        return BindInput.combo(key,glfwModifiers);
    }
    /** A key release during capture: the lone modifier when it was pressed and released alone, else {@link #PENDING}. */
    public static int release(int key){
        if(pendingModifier<0||key!=pendingModifier)return PENDING;
        pendingModifier=-1;
        return key;
    }
    public static void reset(){pendingModifier=-1;}

    /** Button text while capturing: the held modifiers as they are pressed ("Ctrl+…"), or {@code waiting}. */
    public static String prompt(String waiting){
        var mc=Minecraft.getInstance();
        if(mc.getWindow()==null)return waiting;
        long window=mc.getWindow().handle();
        String held=(down(window,GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_KEY_RIGHT_CONTROL)?"Ctrl+":"")
            +(down(window,GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_KEY_RIGHT_SHIFT)?"Shift+":"")
            +(down(window,GLFW.GLFW_KEY_LEFT_ALT,GLFW.GLFW_KEY_RIGHT_ALT)?"Alt+":"");
        return held.isEmpty()?waiting:held+"…";
    }
    private static boolean down(long window,int a,int b){return GLFW.glfwGetKey(window,a)==GLFW.GLFW_PRESS||GLFW.glfwGetKey(window,b)==GLFW.GLFW_PRESS;}
}
