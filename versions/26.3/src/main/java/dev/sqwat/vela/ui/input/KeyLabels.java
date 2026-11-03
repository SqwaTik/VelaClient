package dev.sqwat.vela.ui.input;

import java.util.Locale;

/** Stable compact labels independent of the current Minecraft translation. 26.3 version: keys are SDL scancodes. */
public final class KeyLabels {
    private KeyLabels(){}
    /** "Ctrl+F", "Shift+M4" or a single key name. */
    public static String of(int code,String fallback){
        if(code<0)return "None";
        int mods=BindInput.modifiers(code);
        String prefix=((mods&BindInput.CTRL)!=0?"Ctrl+":"")+((mods&BindInput.SHIFT)!=0?"Shift+":"")+((mods&BindInput.ALT)!=0?"Alt+":"");
        return prefix+single(BindInput.base(code),fallback);
    }
    private static String single(int key,String fallback){
        // SDL numbers mouse buttons from 1 (left), GLFW from 0; the labels stay M1 = left
        if(BindInput.isMouse(key))return "M"+Math.max(1,BindInput.mouseButton(key));
        if(key>=4&&key<=29)return Character.toString((char)('A'+key-4));
        if(key>=30&&key<=38)return Character.toString((char)('1'+key-30));
        if(key==39)return "0";
        if(key>=58&&key<=69)return "F"+(key-57);
        if(key>=104&&key<=115)return "F"+(key-91);
        if(key>=89&&key<=97)return "Num "+(key-88);
        String special=switch(key){
            case 98->"Num 0";case 99->"Num .";case 84->"Num /";case 85->"Num *";case 86->"Num -";case 87->"Num +";case 88->"Num Enter";case 103->"Num =";
            case 52->"'";case 54->",";case 45->"-";case 55->".";case 56->"/";
            case 51->";";case 46->"=";case 47->"[";case 49->"\\";case 48->"]";case 53->"`";case 100->"\\";
            case 71->"Scroll Lock";case 83->"Num Lock";case 70->"Print Screen";case 72->"Pause";
            case 227->"LWin";case 231->"RWin";case 101->"Menu";default->null;
        };
        if(special!=null)return special;
        return switch(key){case 44->"Space";case 41->"Esc";case 40->"Enter";case 43->"Tab";case 42->"Backspace";case 73->"Insert";case 76->"Delete";case 79->"Right";case 80->"Left";case 81->"Down";case 82->"Up";case 75->"Page Up";case 78->"Page Down";case 74->"Home";case 77->"End";case 57->"Caps Lock";case 225->"LShift";case 224->"LCtrl";case 226->"LAlt";case 229->"RShift";case 228->"RCtrl";case 230->"RAlt";default->fallback==null||fallback.isBlank()?"Key "+key:fallback.toUpperCase(Locale.ROOT);};
    }
}
