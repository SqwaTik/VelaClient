package dev.sqwat.vela.ui;

import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.ui.render.Rect;
import dev.sqwat.vela.ui.render.UiViewport;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaIcons;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import java.util.regex.Pattern;

/**
 * Chat privacy: a toggle above the chat input, just left of the Dragmode button. While it is on, everything after /l,
 * /login, /reg or /register in the input is blurred, so a password never appears on screen (recordings, streams). The
 * hidden characters are not drawn at all; the blur only shows how many there are. The text that is sent does not change.
 */
public final class ChatPrivacy {
    public static final int SIZE=dev.sqwat.vela.ui.render.ChatDragGeometry.SIZE;
    private static final Pattern COMMAND=Pattern.compile("(?i)^/(l|login|log|reg|register)(\s|$)");
    private static java.lang.ref.WeakReference<EditBox> input=new java.lang.ref.WeakReference<>(null);
    private ChatPrivacy(){}

    public static void track(EditBox chatInput){input=new java.lang.ref.WeakReference<>(chatInput);}
    public static boolean enabled(){var s=VelaClient.services();return s!=null&&s.configs().current().chatPrivacy;}
    public static void toggle(){var s=VelaClient.services();if(s==null)return;var c=s.configs().current();c.chatPrivacy=!c.chatPrivacy;s.configs().requestSave();}
    public static String label(){return enabled()?"Пароли скрыты":"Пароли видны";}

    /** Index in {@code value} from which characters are hidden, or -1 when nothing is hidden. */
    public static int hiddenFrom(EditBox box,String value){
        if(box!=input.get()||!enabled())return -1;
        var m=COMMAND.matcher(value);
        return m.find()?m.end():-1;
    }
    /** The visible part {@code text} (starting at {@code offset} of the value) with hidden characters as spaces; the blur goes over them. */
    public static String mask(String text,int offset,int hiddenFrom){
        var out=new StringBuilder(text.length());
        for(int i=0;i<text.length();i++){char c=text.charAt(i);out.append(offset+i>=hiddenFrom?' ':c);}
        return out.toString();
    }

    /** The toggle, left of the (expanded) Dragmode button; {@code extension} is how far its label has opened. */
    public static Rect place(Rect drag,int extension){
        return new Rect(Math.max(2,drag.x()-4-SIZE-extension),drag.y(),SIZE+extension,SIZE);
    }
    public static void draw(GuiGraphicsExtractor g,Rect r,float reveal,int labelWidth){
        boolean on=enabled();
        VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),5,VelaTheme.mix(VelaTheme.WINDOW,VelaTheme.SURFACE_HOVER,reveal));
        int iconX=r.x()+r.width()-SIZE;
        if(reveal>.01f){dev.sqwat.vela.ui.render.UiClip.enable(g,r.x(),r.y(),iconX,r.y()+r.height());
            VelaDraw.text(g,label(),iconX-labelWidth+4,r.y()+(r.height()-8)/2,(VelaTheme.TEXT_SECONDARY&0xFFFFFF)|(Math.round(255*reveal)<<24),VelaDraw.Weight.MEDIUM,8);g.disableScissor();}
        VelaIcons.draw(g,on?"key":"visuals",iconX+(SIZE-14)/2,r.y()+(r.height()-14)/2,14,VelaTheme.TEXT_SECONDARY); // same colour as the Dragmode icon next to it
    }

    /**
     * Frosted blur over the hidden characters from {@code x0} to {@code x1}: a soft bar with a smudge per character,
     * {@code cells} wide each. {@code hidden} marks which cells held a character (spaces stay gaps).
     */
    public static void drawBlur(GuiGraphicsExtractor g,int x0,int x1,int y,float cell,boolean[] hidden){
        if(x1<=x0)return;
        VelaDraw.roundedRect(g,x0-2,y-2,x1-x0+4,12,6,0x1CFFFFFF);
        VelaDraw.roundedRect(g,x0-1,y-1,x1-x0+2,10,5,0x14FFFFFF);
        int cy=y+4;
        for(int i=0;i<hidden.length;i++){
            if(!hidden[i])continue;
            int cx=x0+Math.round(cell*(i+.5f));
            for(int r=5;r>=2;r--)VelaDraw.roundedRect(g,cx-r,cy-r,r*2,r*2,r,0x16E8ECF4);
        }
    }
}
