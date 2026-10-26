package dev.sqwat.vela.module.utility;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.ActionSetting;
public final class CommandModule extends ClientModule {
    private final boolean interval;
    public CommandModule(boolean interval){
        super(interval?"interval_commands":"macros",interval?"Interval Commands":"Macros",interval?"Сообщения и команды по таймеру":"Сообщения и команды по клавише",ModuleCategory.UTILITIES);
        this.interval=interval;setting(new ActionSetting("editor","Открыть редактор"));
    }
    public boolean interval(){return interval;}
}
