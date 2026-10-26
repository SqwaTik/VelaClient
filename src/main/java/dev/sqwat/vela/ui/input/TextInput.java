package dev.sqwat.vela.ui.input;

import java.util.function.UnaryOperator;
import org.lwjgl.glfw.GLFW;

/** Shared editing commands; clipboard and validation are supplied by the caller. */
public final class TextInput {
    public interface Clipboard { String get(); void set(String value); }
    private TextInput(){}
    public static boolean key(TextEdit edit,int key,int modifiers,int limit,UnaryOperator<String> filter,Clipboard clipboard){
        boolean ctrl=(modifiers&GLFW.GLFW_MOD_CONTROL)!=0,shift=(modifiers&GLFW.GLFW_MOD_SHIFT)!=0;
        switch(key){
            case GLFW.GLFW_KEY_BACKSPACE -> {if(ctrl)edit.eraseWord();else edit.erase(false);}
            case GLFW.GLFW_KEY_DELETE -> edit.erase(true);
            case GLFW.GLFW_KEY_LEFT -> edit.arrow(-1,shift);
            case GLFW.GLFW_KEY_RIGHT -> edit.arrow(1,shift);
            case GLFW.GLFW_KEY_HOME -> edit.move(0,shift);
            case GLFW.GLFW_KEY_END -> edit.move(edit.text().length(),shift);
            case GLFW.GLFW_KEY_A -> {if(!ctrl)return false;edit.selectAll();}
            case GLFW.GLFW_KEY_C -> {if(!ctrl)return false;clipboard.set(edit.selected());}
            case GLFW.GLFW_KEY_X -> {if(!ctrl)return false;clipboard.set(edit.selected());edit.insert("",limit);}
            case GLFW.GLFW_KEY_V -> {if(!ctrl)return false;edit.insert(filter.apply(TextEdit.cleanPaste(clipboard.get())),limit);}
            default -> {return false;}
        }
        return true;
    }
}
