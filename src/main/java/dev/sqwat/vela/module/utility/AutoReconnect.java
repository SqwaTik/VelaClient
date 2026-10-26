package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import java.util.ArrayDeque;

/**
 * "Reconnect" button on the disconnect screen for the last multiplayer server, with an optional countdown that
 * reconnects automatically. Automatic attempts are capped (5 per 2 minutes) so a ban or full server cannot loop.
 */
public final class AutoReconnect extends ClientModule {
    private static final int MAX_ATTEMPTS=5;
    private static final long WINDOW=120_000_000_000L;
    private final BooleanSetting automatic=setting(new BooleanSetting("automatic","Автоматически","Переподключаться после обратного отсчёта",false));
    private final NumberSetting delay=setting(new NumberSetting("delay","Задержка","Секунд до переподключения",5,2,30,1));
    private final ArrayDeque<Long> attempts=new ArrayDeque<>();
    private ServerData last;
    public AutoReconnect(){
        super("auto_reconnect","Auto Reconnect","Кнопка и отсчёт переподключения при отключении от сервера",ModuleCategory.UTILITIES);
        delay.visibleWhen(automatic::value);
    }
    @Override public void onClientTick(){
        var server=Minecraft.getInstance().getCurrentServer();
        if(server!=null&&Minecraft.getInstance().getConnection()!=null)last=server;
    }
    public static AutoReconnect current(){
        var s=dev.sqwat.vela.VelaClient.services();
        return s!=null&&s.modules().byId("auto_reconnect") instanceof AutoReconnect m&&m.enabled()&&m.last!=null?m:null;
    }
    public String serverIp(){return last.ip;}
    /** Seconds until the automatic attempt, or -1 when it will not happen. */
    public long countdownNanos(){return automatic.value()&&allowed()?delay.value().longValue()*1_000_000_000L:-1;}
    private boolean allowed(){
        long now=System.nanoTime();
        while(!attempts.isEmpty()&&now-attempts.peekFirst()>WINDOW)attempts.pollFirst();
        return attempts.size()<MAX_ATTEMPTS;
    }
    public void reconnect(Screen parent){
        attempts.addLast(System.nanoTime());
        var mc=Minecraft.getInstance();
        ConnectScreen.startConnecting(parent,mc,ServerAddress.parseString(last.ip),last,false,null);
    }
}
