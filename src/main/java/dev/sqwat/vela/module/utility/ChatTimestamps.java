package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import net.minecraft.network.chat.Component;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Prefixes chat lines with the local time they arrived. Only the client's display changes. */
public final class ChatTimestamps extends ClientModule {
    private static final DateTimeFormatter MINUTES=DateTimeFormatter.ofPattern("HH:mm"),SECONDS=DateTimeFormatter.ofPattern("HH:mm:ss");
    private final BooleanSetting seconds=setting(new BooleanSetting("seconds","Секунды","Показывать секунды",false));
    public ChatTimestamps(){super("chat_timestamps","Chat Timestamps","Время получения у каждого сообщения чата",ModuleCategory.UTILITIES);}
    public static Component decorate(Component message){
        var s=dev.sqwat.vela.VelaClient.services();
        if(s==null||!(s.modules().byId("chat_timestamps") instanceof ChatTimestamps module)||!module.enabled()||message==null)return message;
        String time=LocalTime.now().format(module.seconds.value()?SECONDS:MINUTES);
        return Component.literal("["+time+"] ").withStyle(st->st.withColor(0x7F8AA3)).append(message);
    }
}
