package dev.sqwat.vela.core;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.io.*;
import java.util.zip.*;

/** Portable, versioned settings payload. Contains no account storage. */
public final class ConfigShare {
    private static final Gson GSON=new Gson();
    private static final String PREFIX="VELA4:",PREVIOUS="VELA3:";
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
        var compressor=new Deflater(Deflater.BEST_COMPRESSION);
        try{var output=new ByteArrayOutputStream();try(var compressed=new DeflaterOutputStream(output,compressor)){compressed.write(bytes);}return PREFIX+Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray());}catch(IOException error){throw new IllegalStateException("Не удалось создать ключ",error);}finally{compressor.end();}
    }
    public static ClientConfig decode(String key){
        if(key==null||key.length()>90000||!(key.startsWith(PREFIX)||key.startsWith(PREVIOUS)||key.startsWith("VELA2:")||key.startsWith("VELA1:")))throw new IllegalArgumentException("Неверный ключ Vela");
        try{
            byte[] bytes=Base64.getUrlDecoder().decode(key.substring(PREFIX.length()));
            if(key.startsWith(PREFIX)||key.startsWith(PREVIOUS))try(var compressed=new InflaterInputStream(new ByteArrayInputStream(bytes))){bytes=compressed.readNBytes(65537);}
            else if(key.startsWith("VELA2:"))try(var gzip=new GZIPInputStream(new ByteArrayInputStream(bytes))){bytes=gzip.readNBytes(65537);}
            if(bytes.length>65536)throw new IllegalArgumentException();
            ClientConfig config=GSON.fromJson(new String(bytes,StandardCharsets.UTF_8),ClientConfig.class);
            if(config==null||config.schema!=1||config.modules==null||config.modules.size()>1024)throw new IllegalArgumentException();
            return config;
        }catch(RuntimeException|IOException invalid){throw new IllegalArgumentException("Ключ повреждён или имеет неподдерживаемую версию");}
    }
}
