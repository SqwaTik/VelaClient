package dev.sqwat.vela.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ModuleRegistry {
    private final Map<String, ClientModule> modules = new LinkedHashMap<>();
    // Read on every frame from render hooks; registration is rare, so publish immutable copies.
    private volatile Map<String,ClientModule> lookup=Map.of();
    private volatile List<ClientModule> snapshot=List.of();
    private final Map<String,List<ClientModule>> searches=new LinkedHashMap<>();

    public synchronized <T extends ClientModule> T register(T module) {
        if (modules.putIfAbsent(module.id(), module) != null) throw new IllegalArgumentException("Duplicate module: " + module.id());
        snapshot=List.copyOf(modules.values());lookup=Map.copyOf(modules);searches.clear();
        return module;
    }

    public Collection<ClientModule> all() { return snapshot; }

    public synchronized List<ClientModule> find(ModuleCategory category, String query) {
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        String key=category.name()+":"+needle;if(searches.containsKey(key))return searches.get(key);
        var result=snapshot.stream()
            .filter(module -> module.category() == category)
            .filter(module -> needle.isEmpty() || module.name().toLowerCase(Locale.ROOT).contains(needle)
                || module.description().toLowerCase(Locale.ROOT).contains(needle))
            .toList();
        cache(key,result);return result;
    }

    public synchronized List<ClientModule> findAll(String query) {
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) return snapshot;
        String key="all:"+needle;if(searches.containsKey(key))return searches.get(key);
        var result=snapshot.stream()
            .filter(module -> module.name().toLowerCase(Locale.ROOT).contains(needle)
                || module.description().toLowerCase(Locale.ROOT).contains(needle)
                || module.category().title().toLowerCase(Locale.ROOT).contains(needle))
            .toList();
        cache(key,result);return result;
    }
    private void cache(String key,List<ClientModule> result){if(searches.size()>=64)searches.remove(searches.keySet().iterator().next());searches.put(key,result);}

    public ClientModule byId(String id) { return lookup.get(id); }

    public void tickEnabled() {
        for (ClientModule module : snapshot) if (module.enabled()&&module.available()) module.onClientTick();
    }
}
