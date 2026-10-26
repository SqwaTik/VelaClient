package dev.sqwat.vela.render.resource;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Set;
import java.util.zip.ZipEntry;
import static org.junit.jupiter.api.Assertions.*;

class ZipPackIndexTest {
    private static ZipPackIndex index(String... names){
        var sorted=names.clone();java.util.Arrays.sort(sorted);
        var entries=new ZipEntry[sorted.length];for(int i=0;i<sorted.length;i++)entries[i]=new ZipEntry(sorted[i]);
        return new ZipPackIndex(sorted,entries);
    }
    @Test void prefixRangeMatchesAFullScan(){
        var idx=index("assets/minecraft/models/item/a.json","assets/minecraft/models/item/b.json","assets/minecraft/models/block/c.json",
            "assets/minecraft/models/itemx/d.json","assets/minecraft/textures/e.png","assets/custom/models/item/f.json","pack.mcmeta");
        var found=new ArrayList<String>();
        idx.forEachWithPrefix("assets/minecraft/models/item/",(name,entry)->found.add(name));
        assertEquals(java.util.List.of("assets/minecraft/models/item/a.json","assets/minecraft/models/item/b.json"),found);
    }
    @Test void namespacesSkipWholeSubtrees(){
        var idx=index("assets/minecraft/a.json","assets/minecraft/b/c.json","assets/my.pack/x.json","assets/my-pack/y.json","assets/zz/q.json","data/other/z.json");
        assertEquals(Set.of("minecraft","my.pack","my-pack","zz"),idx.namespaces("assets/"));
        assertEquals(Set.of("other"),idx.namespaces("data/"));
    }
}
