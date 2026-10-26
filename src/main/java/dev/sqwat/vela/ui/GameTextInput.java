package dev.sqwat.vela.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import java.util.function.UnaryOperator;

public final class GameTextInput {
    private static final TextInput.Clipboard CLIPBOARD=new TextInput.Clipboard(){
        public String get(){return Minecraft.getInstance().keyboardHandler.getClipboard();}
        public void set(String value){Minecraft.getInstance().keyboardHandler.setClipboard(value);}
    };
    private GameTextInput(){}
    public static boolean key(TextEdit edit,KeyEvent event,int limit,UnaryOperator<String> filter){return TextInput.key(edit,event.key(),event.modifiers(),limit,filter,CLIPBOARD);}
    public static boolean key(TextEdit edit,KeyEvent event,int limit){return key(edit,event,limit,UnaryOperator.identity());}
}
