package dev.sqwat.vela.module.utility;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.NumberSetting;
public final class ItemScroller extends ClientModule {
    private final NumberSetting delay=setting(new NumberSetting("delay_ms","Задержка, мс","Пауза между переносом соседних слотов при удержании Shift + ЛКМ",20,0,100,1));
    public ItemScroller(){super("item_scroller","Item Scroller","Быстрый перенос предметов в контейнере",ModuleCategory.UTILITIES);}
    public int delayMs(){return delay.value().intValue();}
}
