package dev.sqwat.vela;
import dev.sqwat.vela.module.visual.HpCounter;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HpCounterTest {
    @Test void loginHealthHookDoesNotRequireAssignedEntityId()throws Exception{
        try(var in=getClass().getResourceAsStream("/dev/sqwat/vela/module/visual/HpCounter.class")){
            var node=new org.objectweb.asm.tree.ClassNode();
            new org.objectweb.asm.ClassReader(in).accept(node,0);
            var method=node.methods.stream().filter(m->m.name.equals("current")).findFirst().orElseThrow();
            boolean checksRegistration=false,checksThread=false;
            for(var instruction:method.instructions){
                if(instruction instanceof org.objectweb.asm.tree.MethodInsnNode call){
                    assertNotEquals("getId",call.name,"Health initialization precedes entity ID assignment");
                    if(call.name.equals("players"))checksRegistration=true;
                    if(call.name.equals("isSameThread"))checksThread=true;
                }
            }
            assertTrue(checksRegistration);
            assertTrue(checksThread);
        }
    }
    @Test void nativeHealthAndAbsorptionHooksExist()throws Exception{
        try(var in=getClass().getResourceAsStream("/net/minecraft/world/entity/LivingEntity.class")){
            var node=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(in).accept(node,org.objectweb.asm.ClassReader.SKIP_CODE);
            for(String name:java.util.List.of("setHealth","setAbsorptionAmount"))
                assertTrue(node.methods.stream().anyMatch(m->m.name.equals(name)&&m.desc.equals("(F)V")),name);
            assertTrue(node.methods.stream().anyMatch(m->m.name.equals("onSyncedDataUpdated")&&m.desc.equals("(Lnet/minecraft/network/syncher/EntityDataAccessor;)V")));
        }
    }
    @Test void damageAndHealingWithinSameTickAreBothVisibleWithoutDuplicatePolling(){
        var module=new HpCounter();module.setEnabled(true);var id=java.util.UUID.randomUUID();
        module.observe(id,20,Vec3.ZERO,0,true);assertEquals(0,module.count());
        module.observe(id,0,Vec3.ZERO,1,true);module.observe(id,20,Vec3.ZERO,2,true);
        assertEquals(2,module.count());module.observe(id,20,Vec3.ZERO,3,true);assertEquals(2,module.count());
    }
    @Test void numbersAreBoundedAndDisabledStateIsInert(){
        var module=new HpCounter();module.spawn(Vec3.ZERO,-2,0);assertEquals(0,module.count());
        module.setEnabled(true);for(int i=0;i<100;i++)module.spawn(Vec3.ZERO,-2,i);
        assertEquals(HpCounter.LIMIT,module.count());module.setEnabled(false);assertEquals(0,module.count());
    }
    @Test void expiryAndInvalidChanges(){
        var module=new HpCounter();module.setEnabled(true);
        module.spawn(Vec3.ZERO,0,0);module.spawn(Vec3.ZERO,Float.NaN,0);assertEquals(0,module.count());
        module.spawn(Vec3.ZERO,2,0);assertEquals(1,module.count());
        module.expire(HpCounter.LIFETIME);assertEquals(0,module.count());
    }
}
