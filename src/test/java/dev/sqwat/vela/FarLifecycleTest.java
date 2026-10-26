package dev.sqwat.vela;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class FarLifecycleTest {
    @Test void terrainCleanupUsesTheSameCompleteInvalidationAsVanillaReload() throws Exception {
        var far=read("dev/sqwat/vela/module/utility/Far");
        var cleanup=far.methods.stream().filter(m->m.name.equals("clearDisabledTerrain")).findFirst().orElseThrow();
        assertTrue(calls(cleanup,"net/minecraft/client/renderer/extract/LevelExtractor","allChanged"));
        assertFalse(calls(cleanup,"net/minecraft/client/renderer/LevelRenderer","invalidateCompiledGeometry"));
        var extractor=read("net/minecraft/client/renderer/extract/LevelExtractor");
        var reload=extractor.methods.stream().filter(m->m.name.equals("allChanged")).findFirst().orElseThrow();
        boolean dirtyTracker=false,geometry=false;
        for(var instruction:reload.instructions){
            if(instruction instanceof FieldInsnNode field && instruction.getOpcode()==org.objectweb.asm.Opcodes.PUTFIELD){
                dirtyTracker|=field.name.equals("sectionUpdateTracker");
                geometry|=field.name.equals("shouldInvalidateCompiledGeometry");
            }
        }
        assertTrue(dirtyTracker,"Reload must replace the tracker, not keep clean flags for discarded meshes");
        assertTrue(geometry,"Reload must also schedule geometry invalidation");
    }
    private static boolean calls(MethodNode method,String owner,String name){
        for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call&&call.owner.equals(owner)&&call.name.equals(name))return true;
        return false;
    }
    private static ClassNode read(String name)throws Exception{
        try(var stream=FarLifecycleTest.class.getResourceAsStream("/"+name+".class")){
            assertNotNull(stream);var node=new ClassNode();new ClassReader(stream).accept(node,0);return node;
        }
    }
}
