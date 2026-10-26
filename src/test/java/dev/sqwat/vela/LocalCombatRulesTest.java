package dev.sqwat.vela;
import dev.sqwat.vela.module.utility.LocalCombatRules;
import net.minecraft.world.damagesource.CombatRules;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;
class LocalCombatRulesTest {
    @Test void protectionUsesVanillaCapAndSharpnessDoesNotBoostUnenchantedWeapons(){
        assertEquals(0,LocalCombatRules.sharpness(0));
        assertEquals(1,LocalCombatRules.sharpness(1));
        assertEquals(3,LocalCombatRules.sharpness(5));
        assertEquals(3.6f,CombatRules.getDamageAfterMagicAbsorb(10,16),.00001);
        assertEquals(2,CombatRules.getDamageAfterMagicAbsorb(10,100),.00001);
    }
    @Test void exactVanillaAttackHooksExist()throws Exception{
        var player=read("net/minecraft/world/entity/player/Player");
        assertTrue(player.methods.stream().anyMatch(m->m.name.equals("getEnchantedDamage")&&m.desc.equals("(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)F")));
        var living=read("net/minecraft/world/entity/LivingEntity");
        assertTrue(living.methods.stream().anyMatch(m->m.name.equals("getKnockback")&&m.desc.equals("(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)F")));
        var dummy=read("dev/sqwat/vela/module/utility/FakePlayer$LocalDummy");
        var kb=dummy.methods.stream().filter(m->m.name.equals("knockback")).findFirst().orElseThrow();
        assertTrue(calls(kb,"knockback"));assertFalse(calls(kb,"setDeltaMovement"));
        var receive=dummy.methods.stream().filter(m->m.name.equals("receive")).findFirst().orElseThrow();
        assertTrue(calls(receive,"getDamageAfterArmorAbsorb"));assertTrue(calls(receive,"getDamageAfterMagicAbsorb"));
        assertTrue(calls(receive,"dealDefaultKnockback"));assertTrue(calls(receive,"setAbsorptionAmount"));
    }
    private static boolean calls(MethodNode node,String name){for(var op:node.instructions)if(op instanceof MethodInsnNode call&&call.name.equals(name))return true;return false;}
    private static ClassNode read(String name)throws Exception{try(var in=LocalCombatRulesTest.class.getResourceAsStream("/"+name+".class")){assertNotNull(in);var node=new ClassNode();new ClassReader(in).accept(node,0);return node;}}
}
