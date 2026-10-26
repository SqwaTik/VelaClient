package dev.sqwat.vela.module.command;

import dev.sqwat.vela.config.JsonFileStore;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Path;
import java.io.IOException;
import java.util.*;

public final class CommandEntries {
    public record Entry(UUID id,boolean interval,String name,String text,int key,int seconds,boolean enabled) {}
    private final JsonFileStore<List<Entry>> store;
    private final List<Entry> entries=new ArrayList<>();
    public CommandEntries(Path root){
        store=new JsonFileStore<>(root.resolve("commands.json"),new TypeToken<List<Entry>>(){}.getType(),ArrayList::new);
        for(var e:store.load()){if(entries.size()==64)break;try{validate(e);if(entries.stream().noneMatch(old->old.id().equals(e.id())))entries.add(e);}catch(RuntimeException ignored){}}
    }
    public List<Entry> all(){return List.copyOf(entries);}
    public static void validate(Entry e){
        if(e==null||e.id()==null||e.name()==null||e.name().isBlank()||e.name().length()>40)throw new IllegalArgumentException("Название: от 1 до 40 символов");
        if(e.text()==null||e.text().isBlank()||e.text().length()>256||e.text().chars().anyMatch(c->Character.isISOControl(c)||c==0xA7)||e.text().strip().equals("/"))throw new IllegalArgumentException("Введите одну команду или сообщение, до 256 символов");
        if(e.seconds()<1||e.seconds()>86400)throw new IllegalArgumentException("Интервал: от 1 до 86400 секунд");
        if(e.key()<-1||e.key()>348)throw new IllegalArgumentException("Неверная клавиша");
    }
    public void save(Entry e){
        validate(e);var next=new ArrayList<>(entries);int index=-1;
        for(int i=0;i<next.size();i++)if(next.get(i).id().equals(e.id()))index=i;
        if(index>=0)next.set(index,e);else{if(next.size()>=64)throw new IllegalArgumentException("Лимит — 64 записи");next.add(e);}
        persist(next);
    }
    public void remove(UUID id){var next=new ArrayList<>(entries);next.removeIf(e->e.id().equals(id));persist(next);}
    private void persist(List<Entry> next){try{store.save(next);}catch(IOException error){throw new IllegalStateException("Не удалось сохранить команды",error);}entries.clear();entries.addAll(next);}
}
