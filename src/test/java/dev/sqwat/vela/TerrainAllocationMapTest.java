package dev.sqwat.vela;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TerrainAllocationMapTest {
    @Test void repeatedTerrainUploadsAllocateFewerMapObjects(){
        var bean=java.lang.management.ManagementFactory.getThreadMXBean();
        org.junit.jupiter.api.Assumptions.assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        var allocations=(com.sun.management.ThreadMXBean)bean;
        org.junit.jupiter.api.Assumptions.assumeTrue(allocations.isThreadAllocatedMemorySupported());
        allocations.setThreadAllocatedMemoryEnabled(true);
        Object[] keys=new Object[4096];Arrays.setAll(keys,i->new Object());
        Map<Object,Object> original=new HashMap<>(8192),compact=new Object2ObjectOpenHashMap<>(8192);
        churn(original,keys);churn(compact,keys);
        long thread=Thread.currentThread().threadId();
        long start=allocations.getThreadAllocatedBytes(thread);churn(original,keys);
        long before=allocations.getThreadAllocatedBytes(thread)-start;
        start=allocations.getThreadAllocatedBytes(thread);churn(compact,keys);
        long after=allocations.getThreadAllocatedBytes(thread)-start;
        System.out.println("Terrain map allocation bytes: native="+before+", compact="+after);
        assertTrue(after<before/4,"Compact table must eliminate per-entry node allocations");
    }
    private static void churn(Map<Object,Object> map,Object[] keys){
        for(int pass=0;pass<20;pass++){
            for(Object key:keys)map.put(key,key);
            for(Object key:keys)if(map.get(key)!=key)throw new AssertionError();
            for(Object key:keys)map.remove(key);
        }
    }
    private record Key(int id){ @Override public int hashCode(){return id%64;} }
    @Test void allocationReplacementRemovalAndClearPreserveNativeMapSemantics(){
        Map<Key,Integer> nativeMap=new HashMap<>(),compact=new Object2ObjectOpenHashMap<>();
        var random=new Random(30);
        for(int i=0;i<20000;i++){
            Key key=i%101==0?null:new Key(random.nextInt(1000));
            switch(random.nextInt(4)){
                case 0,1 -> assertEquals(nativeMap.put(key,i),compact.put(key,i));
                case 2 -> assertEquals(nativeMap.remove(key),compact.remove(key));
                default -> assertEquals(nativeMap.get(key),compact.get(key));
            }
        }
        assertEquals(nativeMap,compact);
        nativeMap.clear();compact.clear();assertEquals(nativeMap,compact);
    }
    @Test void nativeBufferUsesMapContractAndClearsOnClose()throws Exception{
        try(var input=getClass().getResourceAsStream("/com/mojang/blaze3d/vertex/UberGpuBuffer.class")){
            var node=new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(input).accept(node,0);
            assertTrue(node.fields.stream().anyMatch(f->f.name.equals("allocationMap")&&f.desc.equals("Ljava/util/Map;")));
            var close=node.methods.stream().filter(m->m.name.equals("close")).findFirst().orElseThrow();
            boolean clears=false;
            for(var instruction:close.instructions)if(instruction instanceof org.objectweb.asm.tree.MethodInsnNode call
                &&call.owner.equals("java/util/Map")&&call.name.equals("clear"))clears=true;
            assertTrue(clears);
        }
    }
}
