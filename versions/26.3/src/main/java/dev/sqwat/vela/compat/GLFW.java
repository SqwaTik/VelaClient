package dev.sqwat.vela.compat;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;

/**
 * 26.3 replaced GLFW with SDL3. Vela's sources keep calling {@code org.lwjgl.glfw.GLFW}; the 26.3 build renames that
 * class to this one (versions/26.3/manual.txt). Key constants are SDL scancodes, the values {@code KeyEvent.key()}
 * carries on 26.3; modifiers are SDL {@code KMOD_*} masks as in {@code KeyEvent.modifiers()}; mouse buttons start at 1.
 * The window handle arguments are ignored: SDL tracks the focused window's keyboard and mouse state itself.
 */
public final class GLFW {
    private GLFW(){}

    public static final int GLFW_RELEASE=InputConstants.RELEASE,GLFW_PRESS=InputConstants.PRESS;
    public static final int GLFW_MOD_SHIFT=InputConstants.MOD_SHIFT,GLFW_MOD_CONTROL=InputConstants.MOD_CONTROL,GLFW_MOD_ALT=InputConstants.MOD_ALT,GLFW_MOD_SUPER=InputConstants.MOD_SUPER;
    public static final int GLFW_MOUSE_BUTTON_LEFT=InputConstants.MOUSE_BUTTON_LEFT,GLFW_MOUSE_BUTTON_MIDDLE=InputConstants.MOUSE_BUTTON_MIDDLE,GLFW_MOUSE_BUTTON_RIGHT=InputConstants.MOUSE_BUTTON_RIGHT,GLFW_MOUSE_BUTTON_LAST=InputConstants.MOUSE_BUTTON_8;

    public static final int GLFW_KEY_A=InputConstants.KEY_A,GLFW_KEY_C=InputConstants.KEY_C,GLFW_KEY_D=InputConstants.KEY_D,GLFW_KEY_F=InputConstants.KEY_F,GLFW_KEY_H=InputConstants.KEY_H,
        GLFW_KEY_I=InputConstants.KEY_I,GLFW_KEY_S=InputConstants.KEY_S,GLFW_KEY_V=InputConstants.KEY_V,GLFW_KEY_X=InputConstants.KEY_X,GLFW_KEY_Y=InputConstants.KEY_Y,GLFW_KEY_Z=InputConstants.KEY_Z;
    public static final int GLFW_KEY_1=InputConstants.KEY_1,GLFW_KEY_2=InputConstants.KEY_2,GLFW_KEY_3=InputConstants.KEY_3;
    public static final int GLFW_KEY_SPACE=InputConstants.KEY_SPACE,GLFW_KEY_SLASH=InputConstants.KEY_SLASH,GLFW_KEY_ESCAPE=InputConstants.KEY_ESCAPE,GLFW_KEY_ENTER=InputConstants.KEY_RETURN,
        GLFW_KEY_TAB=InputConstants.KEY_TAB,GLFW_KEY_BACKSPACE=InputConstants.KEY_BACKSPACE,GLFW_KEY_DELETE=InputConstants.KEY_DELETE,GLFW_KEY_HOME=InputConstants.KEY_HOME,GLFW_KEY_END=InputConstants.KEY_END,
        GLFW_KEY_PAGE_UP=InputConstants.KEY_PAGEUP,GLFW_KEY_PAGE_DOWN=InputConstants.KEY_PAGEDOWN,GLFW_KEY_UP=InputConstants.KEY_UP,GLFW_KEY_DOWN=InputConstants.KEY_DOWN,GLFW_KEY_LEFT=InputConstants.KEY_LEFT,GLFW_KEY_RIGHT=InputConstants.KEY_RIGHT;
    public static final int GLFW_KEY_KP_ENTER=InputConstants.KEY_NUMPADENTER,GLFW_KEY_KP_DIVIDE=84;
    public static final int GLFW_KEY_LEFT_CONTROL=InputConstants.KEY_LCONTROL,GLFW_KEY_LEFT_SHIFT=InputConstants.KEY_LSHIFT,GLFW_KEY_LEFT_ALT=InputConstants.KEY_LALT,GLFW_KEY_LEFT_SUPER=InputConstants.KEY_LGUI,
        GLFW_KEY_RIGHT_CONTROL=InputConstants.KEY_RCONTROL,GLFW_KEY_RIGHT_SHIFT=InputConstants.KEY_RSHIFT,GLFW_KEY_RIGHT_ALT=InputConstants.KEY_RALT,GLFW_KEY_RIGHT_SUPER=InputConstants.KEY_RGUI;
    /** Highest SDL scancode (SDL_SCANCODE_COUNT - 1). */
    public static final int GLFW_KEY_LAST=511;
    public static final int GLFW_HAND_CURSOR=SDLMouse.SDL_SYSTEM_CURSOR_POINTER;

    public static int glfwGetKey(long window,int key){return key>=0&&key<=GLFW_KEY_LAST&&InputConstants.isKeyDown(key)?GLFW_PRESS:GLFW_RELEASE;}
    public static int glfwGetMouseButton(long window,int button){
        if(button<1||button>GLFW_MOUSE_BUTTON_LAST)return GLFW_RELEASE;
        return (SDLMouse.nSDL_GetMouseState(0L,0L)&(1<<(button-1)))!=0?GLFW_PRESS:GLFW_RELEASE;
    }
    /** The layout's name for the key at this scancode, or null. */
    public static String glfwGetKeyName(int key,int scancode){
        if(key<0||key>GLFW_KEY_LAST)return null;
        String name=SDLKeyboard.SDL_GetKeyName(SDLKeyboard.SDL_GetKeyFromScancode(key,(short)0,false));
        return name==null||name.isEmpty()?null:name;
    }
    public static long glfwCreateStandardCursor(int shape){return SDLMouse.SDL_CreateSystemCursor(shape);}
    /** {@code cursor} 0 restores the default arrow. */
    public static void glfwSetCursor(long window,long cursor){SDLMouse.SDL_SetCursor(cursor!=0?cursor:SDLMouse.SDL_GetDefaultCursor());}
}
