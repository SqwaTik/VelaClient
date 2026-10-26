package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;

/** Exact ping in milliseconds in the player list instead of the signal bars (mixin.hud.TabPingMixin). */
public final class TabPing extends ClientModule {
    public TabPing(){super("tab_ping","Tab Ping","Точный пинг игроков в списке Tab вместо делений",ModuleCategory.VISUALS);}
    public static boolean active(){
        var s=dev.sqwat.vela.VelaClient.services();
        return s!=null&&s.modules().byId("tab_ping") instanceof TabPing m&&m.enabled();
    }
    /** Green under 80 ms, yellow to 150, orange to 300, red above; grey before the server reports a value. */
    public static int color(int ping){
        if(ping<=0)return 0xFF8996B0;
        return ping<80?0xFF4DE3AE:ping<150?0xFFE8D55A:ping<300?0xFFF0A65E:0xFFFF6178;
    }
}
