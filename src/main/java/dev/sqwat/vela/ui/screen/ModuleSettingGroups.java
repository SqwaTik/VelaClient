package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.setting.BooleanGroup;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ChoiceSetting;
import dev.sqwat.vela.module.setting.Setting;
import java.util.ArrayList;
import java.util.List;

/**
 * How the dashboard lists a module's settings: related flags (particle events, trajectory items, render tweaks) fold
 * into one multi-select, the watermark's metric switches into one picker, and the internal "transparent" switch stays
 * in the HUD editor.
 */
final class ModuleSettingGroups {
    private final VelaServices services;
    ModuleSettingGroups(VelaServices services){this.services=services;}
    ChoiceSetting metrics(){return metricsChoice;}
    boolean isMetrics(Object setting){return setting==metricsChoice;}
    private final java.util.Map<String,BooleanGroup> flagGroups=new java.util.HashMap<>();
    private final ChoiceSetting metricsChoice=new ChoiceSetting("metrics","Показатели","Отображаемые показатели","FPS",List.of("FPS","TPS","Пинг"));
    BooleanSetting metric(String name){String id=name.equals("Пинг")?"ping":name.toLowerCase(java.util.Locale.ROOT);return (BooleanSetting)services.modules().byId("watermark").settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow();}
    List<Setting<?>> visible(ClientModule module){
        if(module.id().equals("particles")){
            var eventIds=java.util.Set.of("hits","jumps","landings","deaths","totems");
            BooleanGroup group=flagGroups.computeIfAbsent(module.id(),id->new BooleanGroup(id+"-events","События",module.settings().stream().filter(s->eventIds.contains(s.id())).map(s->(BooleanSetting)s).toList(),false));
            List<Setting<?>> result=new ArrayList<>();result.add(group);
            for(var setting:module.settings())if(!eventIds.contains(setting.id()))result.add(setting);
            return result;
        }
        if(module.id().equals("trajectories")){
            var itemIds=java.util.Set.of("pearl","snowball","egg","potion","experience");
            BooleanGroup group=flagGroups.computeIfAbsent(module.id(),id->new BooleanGroup(id+"-items","Предметы",module.settings().stream().filter(s->itemIds.contains(s.id())).map(s->(BooleanSetting)s).toList(),false));
            List<Setting<?>> result=new ArrayList<>();result.add(group);
            for(var setting:module.settings())if(!itemIds.contains(setting.id()))result.add(setting);
            return result;
        }
        if(module.id().equals("render_tweaks")){
            BooleanGroup group=flagGroups.computeIfAbsent(module.id(),id->new BooleanGroup(id+"-flags","Скрывать",module.settings().stream().filter(s->s instanceof BooleanSetting).map(s->(BooleanSetting)s).toList(),false));
            List<Setting<?>> result=new ArrayList<>();result.add(group);
            for(var setting:module.settings())if(!(setting instanceof BooleanSetting))result.add(setting);
            return result;
        }
        if(!module.id().equals("watermark"))return module.settings().stream().filter(s->!s.id().equals("transparent")).toList();
        List<Setting<?>> result=new ArrayList<>();result.add(metricsChoice);
        for(var setting:module.settings())if(!List.of("fps","tps","ping","transparent").contains(setting.id()))result.add(setting);
        return result;
    }
}
