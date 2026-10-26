package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ColorSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.module.setting.TextSetting;
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
    private final BooleanSetting links=setting(new BooleanSetting("coordinate_links","Координаты-ссылки","Клик по координатам в сообщении создаёт метку",true));
    private final BooleanSetting mentions=setting(new BooleanSetting("mentions","Упоминания","Подсвечивать ваш ник в чужих сообщениях",true));
    private final BooleanSetting mentionSound=setting(new BooleanSetting("mention_sound","Звук упоминания","Тихий сигнал при упоминании и личном сообщении",true));
    private final ColorSetting mentionColor=setting(new ColorSetting("mention_color","Цвет упоминания",0xFFD166));
    private final BooleanSetting threads=setting(new BooleanSetting("threads","Личные сообщения","Вкладки переписок над строкой чата",true));
    private final TextSetting replyCommand=setting(new TextSetting("reply_command","Команда ЛС","Чем отправлять сообщение во вкладке переписки","msg"));
    private long lastPing;
    private String lastText;
    private GuiMessage lastLine;
    private long lastTime;
    private int count=1;
    public ChatTweaks(){
        super("chat","Chat","Время, упоминания, ссылки на координаты и вкладки ЛС",ModuleCategory.UTILITIES);
        seconds.visibleWhen(timestamps::value);timeColor.visibleWhen(timestamps::value);window.visibleWhen(stack::value);
        mentionSound.visibleWhen(()->mentions.value()||threads.value());mentionColor.visibleWhen(mentions::value);replyCommand.visibleWhen(threads::value);
    }
    @Override protected void onDisable(){lastText=null;lastLine=null;count=1;}

    /**
     * Stacks first (on the raw text), then marks coordinates and mentions, then prefixes the time, so neither the
     * counter nor the timestamp breaks the comparison. The finished line is tagged with its private-message thread.
     */
    public static Component decorate(Component message,List<GuiMessage> lines,boolean[] removed){
        var module=current();if(module==null||message==null)return message;
        String raw=message.getString();
        var thread=module.threads.value()?ChatMarkup.privateMessage(raw):null;
        if(module.stack.value())message=module.stack(message,lines,removed);
        String text=message.getString();
        var found=module.mentions.value()?ChatMarkup.mentions(text,ownName()):List.<ChatMarkup.Span>of();
        message=ChatMarkup.apply(message,module.links.value()?ChatMarkup.coordinates(text):List.of(),found,module.mentionColor.value());
        if((!found.isEmpty()||thread!=null&&thread.incoming())&&module.mentionSound.value())module.ping();
        if(module.timestamps.value()){
            String time=LocalTime.now().format(module.seconds.value()?SECONDS:MINUTES);
            // Siblings, not a child of the time: a child would inherit the time colour.
            message=Component.empty().append(Component.literal("["+time+"] ").withColor(module.timeColor.value())).append(message);
        }
        if(thread!=null)ChatThreads.tag(message,thread.partner(),thread.incoming());
        return message;
    }
    private static String ownName(){var p=net.minecraft.client.Minecraft.getInstance().player;return p==null?null:p.getGameProfile().name();}
    /** A soft UI chime, at most once a second. */
    private void ping(){
        long now=System.currentTimeMillis();if(now-lastPing<1000)return;lastPing=now;
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,1.5f,.45f));
    }
    static boolean threadsEnabled(){var module=current();return module!=null&&module.threads.value();}
    /**
     * Sends a plain chat line typed in an open thread tab as a private message instead. Returns false when the line
     * was handled here (Fabric ALLOW_CHAT).
     */
    public static boolean allowChat(String message){
        String partner=ChatThreads.selected();var module=current();
        if(partner==null||module==null||!module.threads.value()||message.startsWith("/"))return true;
        var connection=net.minecraft.client.Minecraft.getInstance().getConnection();
        if(connection==null)return true;
        connection.sendCommand(module.replyCommand.value().replaceFirst("^/","")+" "+partner+" "+message);
        return false;
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
