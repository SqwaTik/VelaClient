package dev.sqwat.vela.module;
import dev.sqwat.vela.module.setting.*;
import java.util.List;
public final class Animations extends ClientModule {
    private final BooleanSetting chat=setting(new BooleanSetting("chat","Чат","Появление строки ввода и новых сообщений",true));
    private final BooleanSetting tab=setting(new BooleanSetting("tab","Tab","Плавное раскрытие и сворачивание списка игроков",true));
    private final BooleanSetting inventory=setting(new BooleanSetting("inventory","Инвентарь","Появление инвентаря и контейнеров",true));
    private final BooleanSetting hotbar=setting(new BooleanSetting("hotbar","Хотбар","Плавное перемещение выбранной ячейки",true));
    public Animations(){super("animations","Animations","Анимации интерфейса",ModuleCategory.VISUALS);setting(new BooleanGroup("elements","Элементы",List.of(chat,tab,inventory,hotbar),false));for(var s:List.of(chat,tab,inventory,hotbar))s.visibleWhen(()->false);}
    public static boolean active(String part){
        var s=dev.sqwat.vela.VelaClient.services();
        if(s==null||!(s.modules().byId("animations") instanceof Animations a)||!a.enabled())return false;
        return switch(part){case "chat"->a.chat.value();case "tab"->a.tab.value();case "inventory"->a.inventory.value();case "hotbar"->a.hotbar.value();default->false;};
    }
    public static float remaining(long elapsed,long duration){return 1-dev.sqwat.vela.ui.animation.Motion.easeOutCubic(Math.clamp((float)elapsed/duration,0,1));}
}
