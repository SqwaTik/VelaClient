package dev.sqwat.vela.ui;

import java.util.Locale;

/** Stable compact labels independent of the current Minecraft translation. */
public final class KeyLabels {
    private KeyLabels(){}
    public static String of(int key,String fallback){
        if(BindInput.isMouse(key))return "M"+(BindInput.mouseButton(key)+1);
        if(key<0)return "None";
        if(key>=65&&key<=90||key>=48&&key<=57)return Character.toString((char)key);
        if(key>=290&&key<=314)return "F"+(key-289);
        if(key>=320&&key<=329)return "Num "+(key-320);
        String special=switch(key){
            case 39->"'";case 44->",";case 45->"-";case 46->".";case 47->"/";
            case 59->";";case 61->"=";case 91->"[";case 92->"\\";case 93->"]";case 96->"`";
            case 161->"World 1";case 162->"World 2";
            case 281->"Scroll Lock";case 282->"Num Lock";case 283->"Print Screen";case 284->"Pause";
            case 330->"Num .";case 331->"Num /";case 332->"Num *";case 333->"Num -";case 334->"Num +";case 335->"Num Enter";case 336->"Num =";
            case 343->"LWin";case 347->"RWin";case 348->"Menu";default->null;
        };
        if(special!=null)return special;
        return switch(key){case 32->"Space";case 256->"Esc";case 257->"Enter";case 258->"Tab";case 259->"Backspace";case 260->"Insert";case 261->"Delete";case 262->"Right";case 263->"Left";case 264->"Down";case 265->"Up";case 266->"Page Up";case 267->"Page Down";case 268->"Home";case 269->"End";case 280->"Caps Lock";case 340->"LShift";case 341->"LCtrl";case 342->"LAlt";case 344->"RShift";case 345->"RCtrl";case 346->"RAlt";default->fallback==null||fallback.isBlank()?"Key "+key:fallback.toUpperCase(Locale.ROOT);};
    }
}
