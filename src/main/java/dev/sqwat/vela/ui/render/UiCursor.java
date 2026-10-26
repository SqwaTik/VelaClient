package dev.sqwat.vela.ui.render;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
public final class UiCursor {
    private static long hand;
    private UiCursor(){}
    public static void hand(boolean active){
        if(active&&hand==0)hand=GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().handle(),active?hand:0);
    }
}
