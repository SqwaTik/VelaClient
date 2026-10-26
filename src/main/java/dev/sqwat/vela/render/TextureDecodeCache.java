package dev.sqwat.vela.render;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;

/** Bounded native-memory LRU. Each caller owns a copy; resource bytes determine validity. */
public final class TextureDecodeCache {
    private static final long LIMIT=64L*1024*1024;
    private static final LinkedHashMap<String,NativeImage> CACHE=new LinkedHashMap<>(32,.75f,true);
    private static long bytes,hits,misses;
    private TextureDecodeCache(){}
    public static NativeImage read(byte[] encoded)throws IOException {
        if(Boolean.getBoolean("vela.disableTextureDecodeCache")||encoded.length>8*1024*1024)return NativeImage.read(encoded);
        String hash;
        try{hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encoded));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
        synchronized(CACHE){var cached=CACHE.get(hash);if(cached!=null){hits++;return copy(cached);}misses++;}
        NativeImage decoded=NativeImage.read(encoded);
        long size=(long)decoded.getWidth()*decoded.getHeight()*4;
        if(size<=LIMIT/4){
            synchronized(CACHE){
                if(!CACHE.containsKey(hash)){
                    while(bytes+size>LIMIT&&!CACHE.isEmpty()){var iterator=CACHE.entrySet().iterator();var old=iterator.next().getValue();bytes-=(long)old.getWidth()*old.getHeight()*4;iterator.remove();old.close();}
                    CACHE.put(hash,copy(decoded));bytes+=size;
                }
            }
        }
        return decoded;
    }
    private static NativeImage copy(NativeImage original){var result=new NativeImage(original.format(),original.getWidth(),original.getHeight(),false);result.copyFrom(original);return result;}
    public static long[] statistics(){synchronized(CACHE){return new long[]{hits,misses,bytes};}}
    public static void close(){synchronized(CACHE){CACHE.values().forEach(NativeImage::close);CACHE.clear();bytes=0;}}
}
