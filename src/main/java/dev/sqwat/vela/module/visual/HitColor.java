package dev.sqwat.vela.module.visual;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.render.VelaTheme;

/** Player hurt tint inspired by Kimiko; native render state owns the effect lifetime. */
public final class HitColor extends ClientModule {
    private final BooleanSetting mobs=setting(new BooleanSetting("mobs","Мобы","Вспышка и на мобах, не только на игроках",true));
    private final BooleanSetting armor=setting(new BooleanSetting("armor","Закрашивать броню","Применять цвет удара к броне",true));
    public boolean armor(){return armor.value();}
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Цвет игрока при получении урона",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет удара",0xFF6E6E));
    public HitColor(){super("hit_color","Hit Color","Цветовая вспышка на игроках и мобах при получении урона",ModuleCategory.VISUALS);color.visibleWhen(()->!theme.value());}
    public static HitColor current(){return dev.sqwat.vela.module.Modules.active(HitColor.class);}
    public int color(){return theme.value()?VelaTheme.PRIMARY:0xFF000000|color.value();}
    public static boolean applies(boolean player,boolean hurt,float deathTime){return player&&hurt&&deathTime<=0;}
    /** Players always; mobs when the setting allows it. */
    public boolean tints(boolean player,boolean hurt,float deathTime){return applies(player||mobs.value(),hurt,deathTime);}
}
