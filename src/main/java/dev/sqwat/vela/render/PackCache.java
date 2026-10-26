package dev.sqwat.vela.render;

import java.io.IOException;
import java.nio.file.*;

/** Seeds vanilla's download cache on its IO executor. HttpUtil still verifies the full hash. */
public final class PackCache {
    private PackCache() {}
    public static void seed(Path targetDir,String hash,long maxSize) {
        if(hash==null||!hash.matches("[0-9a-f]{40}")||maxSize<=0)return;
        Path target=targetDir.resolve(hash),root=targetDir.getParent();
        if(root==null||Files.exists(target,LinkOption.NOFOLLOW_LINKS)||!Files.isDirectory(root))return;
        try(var dirs=Files.newDirectoryStream(root)) {
            int examined=0;
            for(Path dir:dirs) {
                if(++examined>128)break;
                if(!Files.isDirectory(dir,LinkOption.NOFOLLOW_LINKS)||dir.equals(targetDir))continue;
                Path source=dir.resolve(hash);
                if(!Files.isRegularFile(source,LinkOption.NOFOLLOW_LINKS)||Files.size(source)>maxSize)continue;
                Files.createDirectories(targetDir);
                Files.copy(source,target); // No replace: the ordinary download path owns existing entries.
                return;
            }
        } catch(IOException ignored) {
            // Optional cache reuse must never prevent the normal verified download.
        }
    }
}
