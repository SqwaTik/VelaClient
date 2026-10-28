package dev.sqwat.vela.ui.screen;

import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.setting.BooleanGroup;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.Setting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * How the dashboard lists a module's settings. On/off switches that only say what is shown or for whom (watermark
 * metrics, chat features, particle events, shader effects…) fold into one multi-select at the place of the first of
 * them, so the remaining rows are real settings. The internal "transparent" switch stays in the HUD editor.
 */
final class ModuleSettingGroups {
    private record Group(String id,String label,Set<String> ids){}
    private static final Map<String,List<Group>> GROUPS=Map.of(
        "particles",List.of(new Group("events","События",Set.of("hits","jumps","landings","deaths","totems"))),
        "trajectories",List.of(new Group("items","Предметы",Set.of("pearl","snowball","egg","potion","experience"))),
        "watermark",List.of(new Group("metrics","Показатели",Set.of("fps","bps","tps","ping","time","online"))),
        "chat",List.of(new Group("features","Функции",Set.of("timestamps","stack","coordinate_links","mentions","threads","keep"))),
        "shaders",List.of(new Group("effects","Эффекты",Set.of("sky","hands","wet","fog"))),
        "hitboxes",List.of(new Group("who","Для кого",Set.of("players","friends","self","others"))),
        "pickups",List.of(new Group("show","Показывать",Set.of("losses","containers","totals"))),
        "keystrokes",List.of(new Group("parts","Элементы",Set.of("mouse","cps","space"))));
    private final java.util.Map<String,BooleanGroup> flagGroups=new java.util.HashMap<>();
    ModuleSettingGroups(VelaServices services){}

    List<Setting<?>> visible(ClientModule module){
        List<Group> groups=GROUPS.get(module.id());
        if(module.id().equals("render_tweaks")){
            var flags=module.settings().stream().filter(s->s instanceof BooleanSetting).map(s->(BooleanSetting)s).toList();
            groups=List.of(new Group("flags","Скрывать",Set.copyOf(flags.stream().map(Setting::id).toList())));
        }
        List<Setting<?>> result=new ArrayList<>();
        if(groups==null){
            for(var setting:module.settings())if(!setting.id().equals("transparent"))result.add(setting);
            return result;
        }
        var placed=new java.util.HashSet<String>();
        for(var setting:module.settings()){
            if(setting.id().equals("transparent"))continue;
            Group owner=null;
            for(var group:groups)if(setting instanceof BooleanSetting&&group.ids().contains(setting.id())){owner=group;break;}
            if(owner==null){result.add(setting);continue;}
            if(!placed.add(owner.id()))continue;
            final Group group=owner;
            result.add(flagGroups.computeIfAbsent(module.id()+"-"+group.id(),key->new BooleanGroup(key,group.label(),
                module.settings().stream().filter(s->s instanceof BooleanSetting&&group.ids().contains(s.id())).map(s->(BooleanSetting)s).toList(),false)));
        }
        return result;
    }
}
