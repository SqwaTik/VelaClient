package dev.sqwat.vela;
import dev.sqwat.vela.render.resource.PackCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class PackCacheTest {
    @TempDir Path root;
    private static final String HASH="0123456789012345678901234567890123456789";
    @Test void reusesArchiveAcrossPackIdsWithoutOverwritingExisting() throws Exception {
        var old=Files.createDirectory(root.resolve("old"));Files.writeString(old.resolve(HASH),"zip data");
        var target=root.resolve("new");PackCache.seed(target,HASH,100);
        assertEquals("zip data",Files.readString(target.resolve(HASH)));
        Files.writeString(target.resolve(HASH),"existing");PackCache.seed(target,HASH,100);
        assertEquals("existing",Files.readString(target.resolve(HASH)));
    }
    @Test void skipsUnknownHashesOversizeAndTraversal() throws Exception {
        var old=Files.createDirectory(root.resolve("old"));Files.writeString(old.resolve(HASH),"zip data");
        var target=root.resolve("new");PackCache.seed(target,HASH,1);
        assertFalse(Files.exists(target.resolve(HASH)));
        PackCache.seed(target,null,100);PackCache.seed(target,"../bad",100);
        assertFalse(Files.exists(target));
    }
    @Test void vanillaRejectsCorruptSeedAndReusesVerifiedDownload() throws Exception {
        byte[] data="verified pack".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var function=com.google.common.hash.Hashing.sha1();var hash=function.hashBytes(data);
        var old=Files.createDirectory(root.resolve("old"));Files.writeString(old.resolve(hash.toString()),"corrupt");
        var target=root.resolve("new");PackCache.seed(target,hash.toString(),100);
        var connections=new java.util.concurrent.atomic.AtomicInteger();
        var url=new java.net.URL(null,"http://fixture.invalid/pack",new java.net.URLStreamHandler(){
            protected java.net.URLConnection openConnection(java.net.URL u){return openConnection(u,java.net.Proxy.NO_PROXY);}
            protected java.net.URLConnection openConnection(java.net.URL u,java.net.Proxy proxy){
                connections.incrementAndGet();return new java.net.HttpURLConnection(u){
                    public void connect(){} public void disconnect(){} public boolean usingProxy(){return false;}
                    public java.io.InputStream getInputStream(){return new java.io.ByteArrayInputStream(data);}
                    public long getContentLengthLong(){return data.length;}
                };
            }
        });
        var listener=new net.minecraft.util.HttpUtil.DownloadProgressListener(){
            public void requestStart(){} public void downloadStart(java.util.OptionalLong size){}
            public void downloadedBytes(long bytes){} public void requestFinished(boolean success){assertTrue(success);}
        };
        var result=net.minecraft.util.HttpUtil.downloadFile(target,url,java.util.Map.of(),function,hash,100,java.net.Proxy.NO_PROXY,listener);
        assertArrayEquals(data,Files.readAllBytes(result));assertEquals(1,connections.get());
        net.minecraft.util.HttpUtil.downloadFile(target,url,java.util.Map.of(),function,hash,100,java.net.Proxy.NO_PROXY,listener);
        assertEquals(1,connections.get());
    }
}
