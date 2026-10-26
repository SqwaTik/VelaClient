package dev.sqwat.vela.module;
import net.minecraft.client.Minecraft;
import dev.sqwat.vela.core.VelaServices;
public final class CommandAutomation {
    private static final CommandSchedule SCHEDULE=new CommandSchedule();
    private static Object connection;
    private CommandAutomation(){}
    public static void tick(Minecraft c,VelaServices services){
        if(connection!=c.getConnection()){connection=c.getConnection();SCHEDULE.reset();}
        boolean ready=c.level!=null&&c.player!=null&&c.player.isAlive()&&c.getConnection()!=null&&c.gui.screen()==null&&c.gui.overlay()==null&&c.isWindowActive()&&!c.isPaused();
        var entries=services.commands().all();var ids=new java.util.HashSet<java.util.UUID>();long now=System.nanoTime()/1_000_000;
        for(var e:entries){ids.add(e.id());if(e.interval())continue;var module=services.modules().byId("macros");
            boolean pressed=e.key()>=0&&dev.sqwat.vela.ui.BindInput.down(c.getWindow().handle(),e.key());
            if(SCHEDULE.poll(e,now,ready&&module.enabled(),pressed)){
                String text=e.text().strip();if(text.startsWith("/"))c.getConnection().sendCommand(text.substring(1));else c.getConnection().sendChat(text);
            }
        }
        var timed=SCHEDULE.interval(entries,now,ready&&services.modules().byId("interval_commands").enabled());
        if(timed!=null){String text=timed.text().strip();if(text.startsWith("/"))c.getConnection().sendCommand(text.substring(1));else c.getConnection().sendChat(text);}
        SCHEDULE.retain(ids);
    }
}
