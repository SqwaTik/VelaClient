package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.*;
import java.util.List;

/** Render-facing time and weather overrides; server world state stays untouched. */
public final class Ambience extends ClientModule {
    private final BooleanSetting time=setting(new BooleanSetting("time","Время суток","Изменить отображение времени на клиенте",true));
    private final NumberSetting hour=setting(new NumberSetting("hour","Час","Время по 24-часовой шкале",12,0,24,.5));
    private final ChoiceSetting weather=setting(new ChoiceSetting("weather","Погода","Локальное отображение погоды","Ясно",List.of("Серверная","Ясно","Дождь","Гроза")));
    private final BooleanSetting sky=setting(new BooleanSetting("sky","Цвет неба","Изменить цвет небосвода",false));
    private final BooleanSetting skyTheme=setting(new BooleanSetting("sky_client_color","Цвет клиента","Использовать цвет клиента для неба",true));
    private final ColorSetting skyColor=setting(new ColorSetting("sky_color","Свой цвет неба",0x756CFF));
    public Ambience(){super("ambience","Ambience","Настройка времени суток, погоды и неба",ModuleCategory.VISUALS);hour.visibleWhen(time::value);skyTheme.visibleWhen(sky::value);skyColor.visibleWhen(()->sky.value()&&!skyTheme.value());}
    public int skyColor(int original){return sky.value()?(skyTheme.value()?dev.sqwat.vela.ui.render.VelaTheme.PRIMARY:0xFF000000|skyColor.value()):original;}
    public static Ambience current(){return dev.sqwat.vela.module.Modules.active(Ambience.class);}
    public long clock(long original){return time.value()?Math.floorDiv(original,24000)*24000+clockAtHour(hour.value()):original;}
    public static long clockAtHour(double hour){return Math.floorMod(Math.round(hour*1000)-6000,24000);}
    public float rain(float original){return switch(weather.value()){case "Ясно"->0;case "Дождь","Гроза"->1;default->original;};}
    public float thunder(float original){return switch(weather.value()){case "Ясно","Дождь"->0;case "Гроза"->1;default->original;};}
}
