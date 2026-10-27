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
 * Chat privacy: a toggle at the left above the chat input. While it is on, everything after /l, /login, /reg or
 * /register in the input is shown as asterisks, so a password never appears on screen (recordings, streams). The text that
 * is sent does not change.
 */
public final class ChatPrivacy {
    public static final int SIZE=dev.sqwat.vela.ui.render.ChatDragGeometry.SIZE;
    private static final Pattern COMMAND=Pattern.compile("(?i)^/(l|login|log|reg|register)(\\s|$)");
    private static java.lang.ref.WeakReference<EditBox> input=new java.lang.ref.WeakReference<>(null);
    private ChatPrivacy(){}

    public static void track(EditBox chatInput){input=new java.lang.ref.WeakReference<>(chatInput);}
    public static boolean enabled(){var s=VelaClient.services();return s!=null&&s.configs().current().chatPrivacy;}
    public static void toggle(){var s=VelaClient.services();if(s==null)return;var c=s.configs().current();c.chatPrivacy=!c.chatPrivacy;s.configs().requestSave();}

    /** Index in {@code value} from which characters are hidden, or -1 when nothing is hidden. */
    public static int hiddenFrom(EditBox box,String value){
        if(box!=input.get()||!enabled())return -1;
        var m=COMMAND.matcher(value);
        return m.find()?m.end():-1;
    }
    /** The visible part {@code text} (starting at {@code offset} of the value) with hidden characters as asterisks. */
    public static String mask(String text,int offset,int hiddenFrom){
        var out=new StringBuilder(text.length());
        // '*' is about as wide as most characters, so the caret stays where the real text would put it.
        for(int i=0;i<text.length();i++){char c=text.charAt(i);out.append(offset+i>=hiddenFrom&&c!=' '?'*':c);}
        return out.toString();
    }

    /** Button rectangle in viewport-local coordinates, mirrored to the drag button: left edge above the input. */
    public static Rect place(UiViewport view,int inputX,int inputY){
        int left=(int)Math.floor(view.local(inputX)),top=(int)Math.floor(view.local(inputY));
        return new Rect(Math.max(2,left+1),Math.clamp(top-SIZE-5,2,Math.max(2,view.height()-SIZE-2)),SIZE,SIZE);
    }
    public static void draw(GuiGraphicsExtractor g,Rect r,float hover){
        boolean on=enabled();
        VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),5,VelaTheme.mix(VelaTheme.WINDOW,VelaTheme.SURFACE_HOVER,hover));
        if(on)VelaDraw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),5,VelaTheme.PRIMARY_SOFT);
        VelaIcons.draw(g,on?"key":"visuals",r.x()+(r.width()-14)/2,r.y()+(r.height()-14)/2,14,on?VelaTheme.PRIMARY:VelaTheme.mix(VelaTheme.TEXT_MUTED,VelaTheme.TEXT_SECONDARY,hover));
    }
}
