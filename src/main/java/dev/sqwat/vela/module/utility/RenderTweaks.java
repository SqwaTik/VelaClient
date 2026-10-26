package dev.sqwat.vela.module.utility;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
public final class RenderTweaks extends ClientModule {
    private final java.util.Map<String,BooleanSetting> options=new java.util.HashMap<>();
    public RenderTweaks(){
        super("render_tweaks","Render Tweaks","Скрытие элементов и эффектов",ModuleCategory.UTILITIES);
        add("fire","Огонь на экране");add("scoreboard","Таблица сервера");add("bossbar","Boss Bar");add("particles","Частицы");add("distance_fog","Туман дальности");
        option("dynamic_fov","Динамический FOV","Убирать изменение FOV при беге и использовании предметов");
        add("hurt_camera","Hurt Camera");add("fluid_fog","Лава и вода");
        option("pictures","Картины","Скрывать рисунки карт и картин, сохраняя рамки и основу");
        option("holograms","Голограммы","Скрывать текстовые дисплеи и надписи невидимых стоек");
    }
    private void add(String id,String name){option(id,name,"Скрывать: "+name);}
    private void option(String id,String name,String description){var value=new BooleanSetting(id,name,description,false);options.put(id,value);setting(value);}
    public boolean hides(String id){var option=options.get(id);return enabled()&&option!=null&&Boolean.TRUE.equals(option.value());}
    public static boolean hide(String id){var s=dev.sqwat.vela.VelaClient.services();return s!=null&&s.renderTweaks().hides(id);}
}
