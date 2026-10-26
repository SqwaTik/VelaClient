package dev.sqwat.vela.ui.input;

import org.lwjgl.glfw.GLFW;

/** One persisted encoding for keyboard and all GLFW mouse buttons. */
public final class BindInput {
    private static final int MOUSE_BASE=1000;
    private BindInput(){}
    public static int mouse(int button){return MOUSE_BASE+button;}
    public static boolean isMouse(int key){return key>=MOUSE_BASE&&key<=MOUSE_BASE+GLFW.GLFW_MOUSE_BUTTON_LAST;}
    public static int mouseButton(int key){return key-MOUSE_BASE;}
    public static boolean down(long window,int key){return isMouse(key)?GLFW.glfwGetMouseButton(window,mouseButton(key))==GLFW.GLFW_PRESS:key>=GLFW.GLFW_KEY_SPACE&&key<=GLFW.GLFW_KEY_LAST&&GLFW.glfwGetKey(window,key)==GLFW.GLFW_PRESS;}
    public static boolean clears(int key){return key==GLFW.GLFW_KEY_ESCAPE||key==GLFW.GLFW_KEY_DELETE;}
}
