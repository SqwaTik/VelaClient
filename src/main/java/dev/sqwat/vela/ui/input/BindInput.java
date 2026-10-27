package dev.sqwat.vela.ui.input;

import org.lwjgl.glfw.GLFW;

/**
 * One persisted encoding for keyboard keys, all GLFW mouse buttons and combinations: the low 16 bits hold the key (or
 * mouse code), bits 16-18 the required modifiers in GLFW order (Shift 1, Ctrl 2, Alt 4). Plain keys keep their old
 * values, so existing binds read unchanged; -1 is "none".
 */
public final class BindInput {
    private static final int MOUSE_BASE=1000;
    public static final int SHIFT=1,CTRL=2,ALT=4;
    private BindInput(){}
    public static int mouse(int button){return MOUSE_BASE+button;}
    public static boolean isMouse(int key){key=base(key);return key>=MOUSE_BASE&&key<=MOUSE_BASE+GLFW.GLFW_MOUSE_BUTTON_LAST;}
    public static int mouseButton(int key){return base(key)-MOUSE_BASE;}
    /** Key or mouse code plus the held modifiers; a modifier key alone carries no modifier bits. */
    public static int combo(int key,int glfwModifiers){
        if(key<0||isModifier(key))return key;
        return key|((glfwModifiers&(SHIFT|CTRL|ALT))<<16);
    }
    public static int base(int code){return code<0?code:code&0xFFFF;}
    public static int modifiers(int code){return code<0?0:(code>>>16)&(SHIFT|CTRL|ALT);}
    public static boolean isModifier(int key){return key>=GLFW.GLFW_KEY_LEFT_SHIFT&&key<=GLFW.GLFW_KEY_RIGHT_SUPER;}
    /** Whether the bind is held; a combination also needs its modifiers held (extra ones are allowed). */
    public static boolean down(long window,int code){
        if(code<0)return false;
        int key=base(code);
        boolean pressed=key>=MOUSE_BASE&&key<=MOUSE_BASE+GLFW.GLFW_MOUSE_BUTTON_LAST?GLFW.glfwGetMouseButton(window,key-MOUSE_BASE)==GLFW.GLFW_PRESS
            :key>=GLFW.GLFW_KEY_SPACE&&key<=GLFW.GLFW_KEY_LAST&&GLFW.glfwGetKey(window,key)==GLFW.GLFW_PRESS;
        return pressed&&modifiersHeld(window,modifiers(code));
    }
    public static boolean modifiersHeld(long window,int mods){
        if((mods&CTRL)!=0&&!either(window,GLFW.GLFW_KEY_LEFT_CONTROL,GLFW.GLFW_KEY_RIGHT_CONTROL))return false;
        if((mods&SHIFT)!=0&&!either(window,GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_KEY_RIGHT_SHIFT))return false;
        return (mods&ALT)==0||either(window,GLFW.GLFW_KEY_LEFT_ALT,GLFW.GLFW_KEY_RIGHT_ALT);
    }
    private static boolean either(long window,int a,int b){return GLFW.glfwGetKey(window,a)==GLFW.GLFW_PRESS||GLFW.glfwGetKey(window,b)==GLFW.GLFW_PRESS;}
    /** For key events: same key, and the bind's modifiers are among the held ones. */
    public static boolean matches(int code,int key,int glfwModifiers){
        return code>=0&&base(code)==key&&(modifiers(code)&~glfwModifiers)==0;
    }
    public static boolean clears(int key){return key==GLFW.GLFW_KEY_ESCAPE||key==GLFW.GLFW_KEY_DELETE;}
}
