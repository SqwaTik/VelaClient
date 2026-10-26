package dev.sqwat.vela.core;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.io.*;
import java.util.zip.*;

/** Portable, versioned settings payload. Contains no account storage. */
public final class ConfigShare {
    private static final Gson GSON=new Gson();
    private static final String PREFIX="VELA3:";
    private ConfigShare(){}
    public static String encode(ClientConfig config){
        var payload=GSON.toJsonTree(config).getAsJsonObject();
        var defaults=GSON.toJsonTree(new ClientConfig()).getAsJsonObject();
        for(var entry:defaults.entrySet())if(entry.getValue().equals(payload.get(entry.getKey())))payload.remove(entry.getKey());
        if(payload.has("modules")){
            var moduleDefaults=GSON.toJsonTree(new ClientConfig.ModuleState()).getAsJsonObject();
            for(var module:payload.getAsJsonObject("modules").entrySet()){
                if(!module.getValue().isJsonObject())continue;
                var state=module.getValue().getAsJsonObject();
                for(var entry:moduleDefaults.entrySet())if(entry.getValue().equals(state.get(entry.getKey())))state.remove(entry.getKey());
            }
        }
        byte[] bytes=GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        if(bytes.length>65536)throw new IllegalArgumentException("Конфигурация слишком большая");
        var compressor=new Deflater(Deflater.BEST_COMPRESSION);
        try{var output=new ByteArrayOutputStream();try(var compressed=new DeflaterOutputStream(output,compressor)){compressed.write(bytes);}return PREFIX+Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray());}catch(IOException error){throw new IllegalStateException("Не удалось создать ключ",error);}finally{compressor.end();}
    }
    public static ClientConfig decode(String key){
        if(key==null||key.length()>90000||!(key.startsWith(PREFIX)||key.startsWith("VELA2:")||key.startsWith("VELA1:")))throw new IllegalArgumentException("Неверный ключ Vela");
        try{
            byte[] bytes=Base64.getUrlDecoder().decode(key.substring(PREFIX.length()));
            if(key.startsWith(PREFIX))try(var compressed=new InflaterInputStream(new ByteArrayInputStream(bytes))){bytes=compressed.readNBytes(65537);}
            else if(key.startsWith("VELA2:"))try(var gzip=new GZIPInputStream(new ByteArrayInputStream(bytes))){bytes=gzip.readNBytes(65537);}
            if(bytes.length>65536)throw new IllegalArgumentException();
            ClientConfig config=GSON.fromJson(new String(bytes,StandardCharsets.UTF_8),ClientConfig.class);
            if(config==null||config.schema!=1||config.modules==null||config.modules.size()>1024)throw new IllegalArgumentException();
            return config;
        }catch(RuntimeException|IOException invalid){throw new IllegalArgumentException("Ключ повреждён или имеет неподдерживаемую версию");}
    }
}
