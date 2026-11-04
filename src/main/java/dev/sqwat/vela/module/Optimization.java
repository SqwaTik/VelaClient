package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.Setting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vela's own optimizations, always on (no UI), saved with the config ("optimization") so earlier choices still apply.
 * Each one only skips work whose result cannot be seen (entities behind walls, terrain inside fog) or avoids repeated
 * work (text layout, allocations, lookups); all are on by default and can be switched off to compare. The one that
 * changes the picture is distant_lod, and only past 128 blocks.
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
    private final BooleanSetting fastModels=setting(new BooleanSetting("fast_models","Быстрая сборка моделей",
        "Записывать модели мобов, игроков и предметов в буфер целиком, а не по вершине",true));
    private final BooleanSetting mapAtlas=setting(new BooleanSetting("map_atlas","Атлас карт",
        "Карты в рамках рисуются из общих текстур, стена из карт — за несколько вызовов; карта с прежними пикселями не перезаливается",true));
    private final BooleanSetting distantLod=setting(new BooleanSetting("distant_lod","Упрощение дальних чанков",
        "Дальше 128 блоков листва рисуется сплошной, как в быстрой графике, а трава и цветы не строятся: меньше вершин и проще шейдер; ближе всё как было",true));

    // Off by default: it held "Unlimited" near 250 FPS. A new id, so configs saved while it defaulted to on start off.
    private final BooleanSetting displayCap=setting(new BooleanSetting("display_cap_opt_in","Лимит под монитор",
        "При «Без ограничений» FPS держится на двух частотах монитора: лишние кадры экран всё равно не показывает, а видеокарта меньше греется",false));

    public boolean displayCap(){return displayCap.value();}
    /** The HUD is built at the monitor's refresh rate; always on, no setting. */
    public boolean hudCache(){return true;}
    public boolean fogCulling(){return fogCulling.value();}
    public boolean occludePlayers(){return occludePlayers.value();}
    public boolean occludeMobs(){return occludeMobs.value();}
    public boolean occludeHanging(){return occludeHanging.value();}
    public boolean occludeHolograms(){return occludeHolograms.value();}
    public boolean anyOcclusion(){return occludePlayers.value()||occludeHanging.value()||occludeHolograms.value()||occludeMobs.value();}
    public boolean textCache(){return textCache.value();}
    public boolean fewerAllocations(){return fewerAllocations.value();}
    public boolean lookupCaches(){return lookupCaches.value();}
    public boolean fastModels(){return fastModels.value();}
    public boolean mapAtlas(){return mapAtlas.value();}
    public boolean distantLod(){return distantLod.value();}

    /** The current options, or null before the client has started (unit tests, early mixins): then everything is on. */
    public static Optimization current(){var services=dev.sqwat.vela.VelaClient.services();return services==null?null:services.optimization();}
    public static boolean fewerAllocationsOn(){var o=current();return o==null||o.fewerAllocations();}
    public static boolean lookupCachesOn(){var o=current();return o==null||o.lookupCaches();}
    public static boolean fastModelsOn(){var o=current();return o==null||o.fastModels();}
    public static boolean mapAtlasOn(){var o=current();return o==null||o.mapAtlas();}
    public static boolean distantLodOn(){var o=current();return o==null||o.distantLod();}
}
