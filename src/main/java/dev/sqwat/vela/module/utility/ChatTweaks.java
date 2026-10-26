package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ColorSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * All client-side chat tweaks in one module: arrival time, collapsing repeats into a counter and a longer history.
 * Only the client's chat display changes. Render thread only.
 */
public final class ChatTweaks extends ClientModule {
    private static final DateTimeFormatter MINUTES=DateTimeFormatter.ofPattern("HH:mm"),SECONDS=DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int VANILLA_HISTORY=100;
    private final BooleanSetting timestamps=setting(new BooleanSetting("timestamps","Время","Время получения перед сообщением",true));
    private final BooleanSetting seconds=setting(new BooleanSetting("seconds","Секунды","Показывать секунды",false));
    private final ColorSetting timeColor=setting(new ColorSetting("time_color","Цвет времени",0x7F8AA3));
    private final BooleanSetting stack=setting(new BooleanSetting("stack","Объединять повторы","Одинаковые сообщения подряд — одна строка со счётчиком",true));
    private final NumberSetting window=setting(new NumberSetting("window","Окно повторов","Объединять повторы не старше, секунд",60,5,600,5));
    private final NumberSetting history=setting(new NumberSetting("history","История","Сколько сообщений хранит чат (в игре — 100)",500,100,2000,100));
    private final BooleanSetting keep=setting(new BooleanSetting("keep","Сохранять при выходе","Не очищать чат при отключении от сервера",true));
    private String lastText;
    private GuiMessage lastLine;
    private long lastTime;
    private int count=1;
    public ChatTweaks(){
        super("chat","Chat","Время, счётчик повторов и длинная история",ModuleCategory.UTILITIES);
        seconds.visibleWhen(timestamps::value);timeColor.visibleWhen(timestamps::value);window.visibleWhen(stack::value);
    }
    @Override protected void onDisable(){lastText=null;lastLine=null;count=1;}

    /** Stacks first (on the raw text), then prefixes the time, so the timestamp never breaks the comparison. */
    public static Component decorate(Component message,List<GuiMessage> lines,boolean[] removed){
        var module=current();if(module==null||message==null)return message;
        if(module.stack.value())message=module.stack(message,lines,removed);
        if(!module.timestamps.value())return message;
        String time=LocalTime.now().format(module.seconds.value()?SECONDS:MINUTES);
        // Siblings, not a child of the time: a child would inherit the time colour.
        return Component.empty().append(Component.literal("["+time+"] ").withColor(module.timeColor.value())).append(message);
    }
    /**
     * If the message repeats the newest line, that line is removed from {@code lines} and the returned message carries
     * the counter; the caller must then rebuild the visible lines.
     */
    private Component stack(Component message,List<GuiMessage> lines,boolean[] removed){
        String text=message.getString();long now=System.nanoTime();
        boolean repeat=text.equals(lastText)&&lastLine!=null&&!lines.isEmpty()&&lines.get(0)==lastLine
            &&now-lastTime<=window.value().longValue()*1_000_000_000L;
        lastText=text;lastTime=now;
        if(!repeat){count=1;return message;}
        count++;lines.remove(0);removed[0]=true;
        return Component.empty().append(message).append(Component.literal(" ×"+count).withColor(0x8F9BB3));
    }
    /** Remembers the line that was just added, so only a repeat of the newest line stacks. */
    public static void added(List<GuiMessage> lines){var module=current();if(module!=null)module.lastLine=lines.isEmpty()?null:lines.get(0);}
    /** Whether leaving a server keeps the chat; a divider line then marks where the session ended. */
    public static boolean keepOnDisconnect(){var module=current();return module!=null&&module.keep.value();}
    /** How many messages the chat keeps; vanilla's 100 while the module is off. */
    public static int historyLimit(){var module=current();return module==null?VANILLA_HISTORY:module.history.value().intValue();}
    private static ChatTweaks current(){
        return dev.sqwat.vela.module.Modules.active(ChatTweaks.class);
    }
}
