package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * Collapses a message identical to the previous chat line into that line with a counter ("… ×3"), so server spam
 * does not push the chat away. Only the client's chat display changes. Render thread only.
 */
public final class ChatStack extends ClientModule {
    private final NumberSetting window=setting(new NumberSetting("window","Окно","Объединять повторы не старше, секунд",60,5,600,5));
    private String lastText;
    private GuiMessage lastLine;
    private long lastTime;
    private int count=1;
    public ChatStack(){super("chat_stack","Chat Stack","Повторяющиеся сообщения объединяются с счётчиком",ModuleCategory.UTILITIES);}
    @Override protected void onDisable(){lastText=null;lastLine=null;count=1;}

    /**
     * Called before a message is added. If it repeats the newest line, that line is removed from {@code lines} and the
     * returned message carries the counter; {@link #added} must then rebuild the visible lines.
     */
    public static Component stack(Component message,List<GuiMessage> lines,boolean[] removed){
        var module=current();if(module==null||message==null)return message;
        String text=message.getString();long now=System.nanoTime();
        boolean repeat=text.equals(module.lastText)&&module.lastLine!=null&&!lines.isEmpty()&&lines.get(0)==module.lastLine
            &&now-module.lastTime<=module.window.value().longValue()*1_000_000_000L;
        module.lastText=text;module.lastTime=now;
        if(!repeat){module.count=1;return message;}
        module.count++;lines.remove(0);removed[0]=true;
        return message.copy().append(Component.literal(" ×"+module.count).withStyle(st->st.withColor(0x8F9BB3)));
    }
    /** Remembers the line that was just added, so only a repeat of the newest line stacks. */
    public static void added(List<GuiMessage> lines){var module=current();if(module!=null)module.lastLine=lines.isEmpty()?null:lines.get(0);}
    private static ChatStack current(){
        var s=dev.sqwat.vela.VelaClient.services();
        return s!=null&&s.modules().byId("chat_stack") instanceof ChatStack m&&m.enabled()?m:null;
    }
}
