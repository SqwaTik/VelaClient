package dev.sqwat.vela;
import dev.sqwat.vela.module.ExplosionWave;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExplosionWaveTest {
    @Test void cappedExpiredAndDisabled(){
        var module=new ExplosionWave();module.spawn(Vec3.ZERO,0);assertEquals(0,module.count());
        module.setEnabled(true);for(int i=0;i<100;i++)module.spawn(Vec3.ZERO,i);
        assertEquals(4,module.count());module.expire(4_000_000_000L);assertEquals(0,module.count());
        module.spawn(Vec3.ZERO,5_000_000_000L);module.setEnabled(false);assertEquals(0,module.count());
    }
    @Test void nativeEventExists()throws Exception{
        try(var in=getClass().getResourceAsStream("/net/minecraft/client/multiplayer/ClientPacketListener.class")){
            var node=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(in).accept(node,org.objectweb.asm.ClassReader.SKIP_CODE);
            assertTrue(node.methods.stream().anyMatch(m->m.name.equals("handleExplosion")&&m.desc.equals("(Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;)V")));
        }
    }
}
