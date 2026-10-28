package dev.sqwat.vela.module.setting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Items with their own colour, saved as "minecraft:diamond=55CFFF;…" (at most 128 valid entries). */
public final class ItemColorsSetting extends Setting<String> {
    private static final Pattern ENTRY=Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)=([0-9A-Fa-f]{6})");
    private String parsedFrom;
    private Map<String,Integer> parsed=Map.of();
    public ItemColorsSetting(String id,String name,String description){super(id,name,description,"");}
    @Override protected String validate(String value){
        var out=new StringBuilder();int count=0;
        for(String part:value.split(";")){
            var m=ENTRY.matcher(part.strip());
            if(!m.matches()||count>=128)continue;
            if(!out.isEmpty())out.append(';');
            out.append(m.group(1)).append('=').append(m.group(2).toUpperCase(java.util.Locale.ROOT));count++;
        }
        return out.toString();
    }
    /** Item id → RGB, in the order they were added. */
    public synchronized Map<String,Integer> colors(){
        if(!value().equals(parsedFrom)){
            var map=new LinkedHashMap<String,Integer>();
            if(!value().isEmpty())for(String part:value().split(";")){int eq=part.indexOf('=');map.put(part.substring(0,eq),Integer.parseInt(part.substring(eq+1),16));}
            parsed=java.util.Collections.unmodifiableMap(map);parsedFrom=value();
        }
        return parsed;
    }
    public void put(String item,int rgb){var map=new LinkedHashMap<>(colors());map.put(item,rgb&0xFFFFFF);store(map);}
    public void remove(String item){var map=new LinkedHashMap<>(colors());map.remove(item);store(map);}
    private void store(Map<String,Integer> map){
        var out=new StringBuilder();
        for(var e:map.entrySet()){if(!out.isEmpty())out.append(';');out.append(e.getKey()).append('=').append(String.format("%06X",e.getValue()));}
        set(out.toString());
    }
}
