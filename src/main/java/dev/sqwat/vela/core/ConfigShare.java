package dev.sqwat.vela.core;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.io.*;
import java.util.zip.*;

/** Portable, versioned settings payload. Contains no account storage. */
public final class ConfigShare {
    private static final Gson GSON=new Gson();
    /** Current keys: raw deflate with {@link ConfigDictionary#V5} as preset dictionary. */
    private static final String PREFIX="V5",PREVIOUS="VELA4:",OLDER="VELA3:";
    /** Where the menu is scrolled to and what is open in it is nobody else's business: never shared. */
    private static final java.util.Set<String> VIEW_STATE=java.util.Set.of("lastSection","expandedModule","expandedModules","friendOptionsOpen","settingsTab","lastSeenChangelog","activeProfile","friendVisualsExpanded");
    private ConfigShare(){}
    public static String encode(ClientConfig config){return encode(config,java.util.Map.of());}
    /**
     * Only what differs from a fresh config travels: default top-level fields, default module flags and every module
     * setting still at its default are left out, and modules that end up empty disappear.
     */
    public static String encode(ClientConfig config,java.util.Map<String,java.util.Map<String,com.google.gson.JsonElement>> settingDefaults){
        var payload=GSON.toJsonTree(config).getAsJsonObject();
        var defaults=GSON.toJsonTree(new ClientConfig()).getAsJsonObject();
        for(var entry:defaults.entrySet())if(entry.getValue().equals(payload.get(entry.getKey())))payload.remove(entry.getKey());
        VIEW_STATE.forEach(payload::remove);
        if(payload.has("modules")){
            var moduleDefaults=GSON.toJsonTree(new ClientConfig.ModuleState()).getAsJsonObject();
            for(var module:payload.getAsJsonObject("modules").entrySet()){
                if(!module.getValue().isJsonObject())continue;
                var state=module.getValue().getAsJsonObject();
                for(var entry:moduleDefaults.entrySet())if(entry.getValue().equals(state.get(entry.getKey())))state.remove(entry.getKey());
                var moduleSettingDefaults=settingDefaults.get(module.getKey());
                if(moduleSettingDefaults!=null&&state.has("settings")&&state.get("settings").isJsonObject()){
                    var settings=state.getAsJsonObject("settings");
                    for(var entry:new java.util.ArrayList<>(settings.entrySet()))if(entry.getValue().equals(moduleSettingDefaults.get(entry.getKey())))settings.remove(entry.getKey());
                    if(settings.size()==0)state.remove("settings");
                }
            }
            var modules=payload.getAsJsonObject("modules");
            for(var name:new java.util.ArrayList<>(modules.keySet()))if(modules.get(name).isJsonObject()&&modules.getAsJsonObject(name).size()==0)modules.remove(name);
        }
        byte[] bytes=GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        if(bytes.length>65536)throw new IllegalArgumentException("Конфигурация слишком большая");
        // Raw deflate (no header or checksum: JSON that doesn't parse is caught anyway) primed with the names configs use.
        var compressor=new Deflater(Deflater.BEST_COMPRESSION,true);
        try{
            compressor.setDictionary(ConfigDictionary.V5);
            var output=new ByteArrayOutputStream();try(var compressed=new DeflaterOutputStream(output,compressor)){compressed.write(bytes);}
            return PREFIX+Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray());
        }catch(IOException error){throw new IllegalStateException("Не удалось создать ключ",error);}finally{compressor.end();}
    }
    public static ClientConfig decode(String key){
        boolean current=key!=null&&key.startsWith(PREFIX);
        if(key==null||key.length()>90000||!(current||key.startsWith(PREVIOUS)||key.startsWith(OLDER)||key.startsWith("VELA2:")||key.startsWith("VELA1:")))throw new IllegalArgumentException("Неверный ключ Vela");
        try{
            byte[] bytes=Base64.getUrlDecoder().decode(key.substring(current?PREFIX.length():PREVIOUS.length()));
            if(current)bytes=inflateRaw(bytes);
            else if(key.startsWith(PREVIOUS)||key.startsWith(OLDER))try(var compressed=new InflaterInputStream(new ByteArrayInputStream(bytes))){bytes=compressed.readNBytes(65537);}
            else if(key.startsWith("VELA2:"))try(var gzip=new GZIPInputStream(new ByteArrayInputStream(bytes))){bytes=gzip.readNBytes(65537);}
            if(bytes.length>65536)throw new IllegalArgumentException();
            ClientConfig config=GSON.fromJson(new String(bytes,StandardCharsets.UTF_8),ClientConfig.class);
            if(config==null||config.schema!=1||config.modules==null||config.modules.size()>1024)throw new IllegalArgumentException();
            return config;
        }catch(RuntimeException|IOException|DataFormatException invalid){throw new IllegalArgumentException("Ключ повреждён или имеет неподдерживаемую версию");}
    }
    /** At most 64 KiB + 1 out, so a key can't blow up into gigabytes; a key cut short is an error, not half a config. */
    private static byte[] inflateRaw(byte[] data)throws DataFormatException{
        var inflater=new Inflater(true);
        try{
            inflater.setDictionary(ConfigDictionary.V5);
            // Raw streams need one byte past the end to finish.
            inflater.setInput(java.util.Arrays.copyOf(data,data.length+1));
            var out=new ByteArrayOutputStream();byte[] chunk=new byte[4096];
            while(!inflater.finished()){
                int n=inflater.inflate(chunk);
                if(n==0&&(inflater.needsInput()||inflater.needsDictionary()))throw new DataFormatException("truncated");
                out.write(chunk,0,n);
                if(out.size()>65536)throw new DataFormatException("too large");
            }
            return out.toByteArray();
        }finally{inflater.end();}
    }
}
