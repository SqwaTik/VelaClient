package dev.sqwat.vela.module;

import java.util.*;

/**
 * Known server projects whose published rules forbid some Vela functions. On joining one of them the forbidden
 * modules are switched off and hidden for the session; on leaving, the ones the player had on come back.
 */
public final class ServerRules {
    public record Project(String name,List<String> domains,Map<String,String> forbidden){}

    private static final Map<String,String> COMMON=Map.of(
        "click_pearl","бросок по клавише с возвратом слота — макрос",
        "crystal_optimizer","ускорение кристаллов — запрещено как ClickCrystal / CrystalOptimizer",
        "macros","макросы",
        "interval_commands","автоматизация команд");
    private static Map<String,String> with(Map<String,String> extra){var all=new LinkedHashMap<>(COMMON);all.putAll(extra);return Collections.unmodifiableMap(all);}

    public static final List<Project> PROJECTS=List.of(
        new Project("ReallyWorld",List.of("reallyworld.ru","playrw.ru","reallyworld.me","reallyworld.online","reallyworld.org"),with(Map.of(
            "freelook","п. 5.3: FreeCam",
            "hitboxes","п. 5.3: подсветка игроков и изменение хитбоксов"))),
        new Project("FunTime",List.of("funtime.su","funtime.me"),COMMON),
        new Project("HolyWorld",List.of("holyworld.ru","holyworld.me"),with(Map.of(
            "trajectories","п. 2.4: Trajectories / Predict",
            "hitboxes","п. 2.4: ESP — подсветка хитбокса игрока",
            "near","п. 2.4: трейсеры и указатели на игроков",
            "far","п. 2.4: сохранение мира на диск"))));

    private static volatile Project active;
    private static final Set<String> switchedOff=new LinkedHashSet<>();
    private ServerRules(){}

    /** The project serving {@code address} ("mc.reallyworld.ru:25565"), or null. */
    public static Project match(String address){
        if(address==null)return null;
        String host=address.strip().toLowerCase(Locale.ROOT);
        if(host.startsWith("["))return null;
        int colon=host.indexOf(':');if(colon>=0)host=host.substring(0,colon);
        while(host.endsWith("."))host=host.substring(0,host.length()-1);
        for(var project:PROJECTS)for(var domain:project.domains)if(host.equals(domain)||host.endsWith("."+domain))return project;
        return null;
    }
    public static boolean blocked(String moduleId){var p=active;return p!=null&&p.forbidden.containsKey(moduleId);}
    public static Project active(){return active;}

    /** Applies the project's rules; returns the names of the modules that were on and are now off. */
    public static synchronized List<String> join(String address,ModuleRegistry modules){
        leave(modules);
        var project=match(address);if(project==null)return List.of();
        List<String> names=new ArrayList<>();
        for(var id:project.forbidden.keySet()){
            var module=modules.byId(id);
            if(module!=null&&module.enabled()){switchedOff.add(id);module.setEnabled(false);names.add(module.name());}
        }
        active=project;
        return names;
    }
    /**
     * Functions that can give an edge in multiplayer; a server's "*" request switches all of them off. The ids of
     * every module are accepted too, so a server can also turn off purely visual ones it does not want.
     */
    public static final Set<String> COMPETITIVE=Set.of("click_pearl","crystal_optimizer","macros","interval_commands","near",
        "auto_tpaccept","freelook","hitboxes","trajectories","far","gamma");

    /**
     * A server's own opt-out (the velaclient:disable channel): the listed modules are switched off and hidden until
     * the player leaves, on top of any rules already known for that server. Returns the names switched off.
     */
    public static synchronized List<String> serverRequest(List<String> ids,ModuleRegistry modules){
        Map<String,String> forbidden=new LinkedHashMap<>(active==null?Map.of():active.forbidden);
        for(var id:ids){
            if(id.equals("*"))for(var c:COMPETITIVE)forbidden.putIfAbsent(c,"server request");
            else if(modules.byId(id)!=null)forbidden.putIfAbsent(id,"server request");
        }
        List<String> names=new ArrayList<>();
        for(var id:forbidden.keySet()){
            var module=modules.byId(id);
            if(module!=null&&module.enabled()){switchedOff.add(id);module.setEnabled(false);names.add(module.name());}
        }
        active=new Project(active==null?"Server":active.name,active==null?List.of():active.domains,Collections.unmodifiableMap(forbidden));
        return names;
    }
    public static synchronized void leave(ModuleRegistry modules){
        if(active==null)return;
        active=null;
        for(var id:switchedOff){var module=modules.byId(id);if(module!=null)module.setEnabled(true);}
        switchedOff.clear();
    }
}
