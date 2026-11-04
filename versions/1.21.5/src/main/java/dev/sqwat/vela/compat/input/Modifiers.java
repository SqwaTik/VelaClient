package dev.sqwat.vela.compat.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** 1.21.5 screen callbacks carry no modifier bits for clicks: they are read from the keyboard. */
public final class Modifiers {
    private Modifiers(){}
    public static int current(){
        long window=Minecraft.getInstance().getWindow().getWindow();
        int bits=0;
        if(InputConstants.isKeyDown(window,GLFW.GLFW_KEY_LEFT_SHIFT)||InputConstants.isKeyDown(window,GLFW.GLFW_KEY_RIGHT_SHIFT))bits|=GLFW.GLFW_MOD_SHIFT;
        if(InputConstants.isKeyDown(window,GLFW.GLFW_KEY_LEFT_CONTROL)||InputConstants.isKeyDown(window,GLFW.GLFW_KEY_RIGHT_CONTROL))bits|=GLFW.GLFW_MOD_CONTROL;
        if(InputConstants.isKeyDown(window,GLFW.GLFW_KEY_LEFT_ALT)||InputConstants.isKeyDown(window,GLFW.GLFW_KEY_RIGHT_ALT))bits|=GLFW.GLFW_MOD_ALT;
        if(InputConstants.isKeyDown(window,GLFW.GLFW_KEY_LEFT_SUPER)||InputConstants.isKeyDown(window,GLFW.GLFW_KEY_RIGHT_SUPER))bits|=GLFW.GLFW_MOD_SUPER;
        return bits;
    }
    static boolean shift(int modifiers){return (modifiers&GLFW.GLFW_MOD_SHIFT)!=0;}
    static boolean control(int modifiers){return (modifiers&(net.minecraft.Util.getPlatform()==net.minecraft.Util.OS.OSX?GLFW.GLFW_MOD_SUPER:GLFW.GLFW_MOD_CONTROL))!=0;}
    static boolean alt(int modifiers){return (modifiers&GLFW.GLFW_MOD_ALT)!=0;}
}
