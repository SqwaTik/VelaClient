package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.utility.StorageStore.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StorageStoreTest {
    private static Container chest(int x,long seen,Stack... items){return new Container(new Key("minecraft:overworld",x,64,0),Kind.CHEST,seen,List.of(items));}
    @Test void searchMergesAcrossContainersNewestFirst(){
        var store=new StorageStore();
        store.put(chest(1,100,new Stack("minecraft:diamond","",5,false),new Stack("minecraft:stone","",64,false)));
        store.put(chest(2,200,new Stack("minecraft:diamond","",3,true)));
        var hits=store.search("алмаз",id->id.equals("minecraft:diamond")?"Алмаз":"Камень");
        assertEquals(1,hits.size());assertEquals(8,hits.get(0).total());
        assertEquals(2,hits.get(0).containers().get(0).key().x()); // seen later comes first
        assertEquals(2,store.search("",id->id).size());
        assertEquals(1,store.search("DIAMOND",id->id).size()); // id search, any case
    }
    @Test void reopeningReplacesAndCapacityDropsOldest(){
        var store=new StorageStore();
        store.put(chest(1,1,new Stack("minecraft:dirt","",1,false)));
        store.put(chest(1,2,new Stack("minecraft:sand","",2,false)));
        assertEquals(1,store.size());assertEquals("minecraft:sand",store.all().iterator().next().items().get(0).item());
        for(int i=0;i<StorageStore.MAX_CONTAINERS+5;i++)store.put(chest(100+i,10+i,new Stack("minecraft:dirt","",1,false)));
        assertEquals(StorageStore.MAX_CONTAINERS,store.size());
        assertNull(store.get(new Key("minecraft:overworld",1,64,0)));
    }
    @Test void roundTripsThroughCompactFile(@TempDir Path dir)throws Exception{
        var store=new StorageStore();
        for(int i=0;i<500;i++)store.put(chest(i,i,new Stack("minecraft:diamond","Мой меч",i+1,i%2==0),new Stack("minecraft:stone","",64,false)));
        var file=dir.resolve("s.bin");store.save(file);
        var loaded=StorageStore.load(file);
        assertEquals(500,loaded.size());
        assertEquals(store.get(new Key("minecraft:overworld",7,64,0)),loaded.get(new Key("minecraft:overworld",7,64,0)));
        assertTrue(java.nio.file.Files.size(file)<40_000,"500 chests should stay small: "+java.nio.file.Files.size(file));
    }
}
