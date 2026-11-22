package dev.sqwat.vela.compat.input;

import org.lwjgl.glfw.GLFW;

/** 1.21.9's key record; 1.21.5's screen methods take (key, scancode, modifiers), turned into one of these (see manual.txt). */
public record KeyEvent(int key,int scancode,int modifiers) {
    public int input(){return key;}
    public boolean hasShiftDown(){return Modifiers.shift(modifiers);}
    public boolean hasControlDown(){return Modifiers.control(modifiers);}
    public boolean hasAltDown(){return Modifiers.alt(modifiers);}
    public boolean isEscape(){return key==GLFW.GLFW_KEY_ESCAPE;}
    public boolean isConfirmation(){return key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_SPACE||key==GLFW.GLFW_KEY_KP_ENTER;}
    public boolean isSelection(){return key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_SPACE||key==GLFW.GLFW_KEY_KP_ENTER;}
    public boolean isCycleFocus(){return key==GLFW.GLFW_KEY_TAB;}
    public boolean isLeft(){return key==GLFW.GLFW_KEY_LEFT;}
    public boolean isRight(){return key==GLFW.GLFW_KEY_RIGHT;}
    public boolean isUp(){return key==GLFW.GLFW_KEY_UP;}
    public boolean isDown(){return key==GLFW.GLFW_KEY_DOWN;}
    public boolean isSelectAll(){return key==GLFW.GLFW_KEY_A&&hasControlDown()&&!hasShiftDown()&&!hasAltDown();}
    public boolean isCopy(){return key==GLFW.GLFW_KEY_C&&hasControlDown()&&!hasShiftDown()&&!hasAltDown();}
    public boolean isPaste(){return key==GLFW.GLFW_KEY_V&&hasControlDown()&&!hasShiftDown()&&!hasAltDown();}
    public boolean isCut(){return key==GLFW.GLFW_KEY_X&&hasControlDown()&&!hasShiftDown()&&!hasAltDown();}
}
