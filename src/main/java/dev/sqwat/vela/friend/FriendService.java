package dev.sqwat.vela.friend;

import com.google.gson.reflect.TypeToken;
import dev.sqwat.vela.core.JsonFileStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class FriendService {
    private final JsonFileStore<List<FriendEntry>> store;
    private final List<FriendEntry> friends;
    private final java.util.Set<String> names=new java.util.HashSet<>();
    private List<FriendEntry> sorted;

    public FriendService(Path root) {
        store = new JsonFileStore<>(root.resolve("friends.json"), new TypeToken<List<FriendEntry>>() {}.getType(), ArrayList::new);
        friends = new ArrayList<>(store.load());
        reindex();
    }

    public synchronized List<FriendEntry> all() {
        return sorted;
    }

    public synchronized boolean add(String name) {
        String clean = name == null ? "" : name.strip();
        if (!clean.matches("[A-Za-z0-9_]{3,16}") || contains(clean)) return false;
        friends.add(new FriendEntry(UUID.randomUUID(), clean, "", "", false, System.currentTimeMillis()));
        save();
        return true;
    }

    public synchronized void toggleFavorite(UUID id){for(int i=0;i<friends.size();i++){var f=friends.get(i);if(f.id().equals(id)){friends.set(i,new FriendEntry(f.id(),f.name(),f.alias(),f.note(),!f.favorite(),f.addedAt()));save();return;}}}
    public synchronized boolean remove(UUID id) {
        boolean changed = friends.removeIf(friend -> friend.id().equals(id));
        if (changed) save();
        return changed;
    }

    public synchronized boolean contains(String name) {
        if (name == null) return false;
        String needle = name.toLowerCase(Locale.ROOT);
        return names.contains(needle);
    }

    private void reindex(){
        names.clear();for(var friend:friends)names.add(friend.name().toLowerCase(Locale.ROOT));
        sorted=friends.stream().sorted(Comparator.comparing(FriendEntry::favorite).reversed().thenComparing(FriendEntry::name,String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private void save() {
        reindex();
        try { store.save(friends); } catch (IOException ignored) { }
    }
}
