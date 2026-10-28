package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vela's own optimizations, always on (no UI), saved with the config ("optimization") so earlier choices still apply.
 * Each one only skips work whose result cannot be seen (entities behind walls, terrain inside fog) or avoids repeated
 * work (text layout, allocations, lookups); all are on by default and can be switched off to compare.
 * Setting ids are the ones earlier versions used, so saved choices carry over.
 */
public final class Optimization {
    private final List<Setting<?>> settings=new ArrayList<>();
    private <T extends Setting<?>> T setting(T value){settings.add(value);return value;}
    public List<Setting<?>> settings(){return Collections.unmodifiableList(settings);}

    private final BooleanSetting fogCulling=setting(new BooleanSetting("fog_culling","Отсечение тумана",
        "Не рисовать рельеф, полностью скрытый туманом (Незер, лава, слепота)",true));
    private final BooleanSetting occludePlayers=setting(new BooleanSetting("occlusion_players","Игроки за стенами",
        "Не рисовать модели, броню и предметы игроков за блоками. Ники остаются",true));
    private final BooleanSetting occludeMobs=setting(new BooleanSetting("occlusion_mobs","Мобы за стенами",
        "Не рисовать мобов без имени за блоками",true));
    private final BooleanSetting occludeHanging=setting(new BooleanSetting("occlusion_hanging","Картины и рамки за стенами",
        "Не рисовать картины, рамки и карты за блоками",true));
    private final BooleanSetting occludeHolograms=setting(new BooleanSetting("occlusion_holograms","Голограммы за стенами",
        "Не рисовать текстовые дисплеи и стойки за блоками. Дисплеи «сквозь стены» остаются",true));
    private final BooleanSetting textCache=setting(new BooleanSetting("diag_text_cache","Кэш раскладки текста",
        "Повторно использовать раскладку неподвижного текста интерфейса",true));
    private final BooleanSetting fewerAllocations=setting(new BooleanSetting("micro_opts","Меньше выделений памяти",
        "Повторно использовать списки при сортировке интерфейса и сборке рельефа",true));
    private final BooleanSetting lookupCaches=setting(new BooleanSetting("lookup_caches","Быстрый поиск групп отрисовки",
        "Индекс вместо перебора списка при сборке кадра",true));

    public boolean fogCulling(){return fogCulling.value();}
    public boolean occludePlayers(){return occludePlayers.value();}
    public boolean occludeMobs(){return occludeMobs.value();}
    public boolean occludeHanging(){return occludeHanging.value();}
    public boolean occludeHolograms(){return occludeHolograms.value();}
    public boolean anyOcclusion(){return occludePlayers.value()||occludeHanging.value()||occludeHolograms.value()||occludeMobs.value();}
    public boolean textCache(){return textCache.value();}
    public boolean fewerAllocations(){return fewerAllocations.value();}
    public boolean lookupCaches(){return lookupCaches.value();}

    /** The current options, or null before the client has started (unit tests, early mixins): then everything is on. */
    public static Optimization current(){var services=dev.sqwat.vela.VelaClient.services();return services==null?null:services.optimization();}
    public static boolean fewerAllocationsOn(){var o=current();return o==null||o.fewerAllocations();}
    public static boolean lookupCachesOn(){var o=current();return o==null||o.lookupCaches();}
}
