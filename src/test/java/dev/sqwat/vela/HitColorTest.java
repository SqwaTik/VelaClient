package dev.sqwat.vela;
import dev.sqwat.vela.module.visual.HitColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HitColorTest {
    @Test void armorSubmissionHasTintArgument()throws Exception{
        try(var in=getClass().getResourceAsStream("/net/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer.class")){
            var node=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(in).accept(node,0);
            var method=node.methods.stream().filter(m->m.name.equals("renderLayers")&&m.desc.contains("ILnet/minecraft/resources/Identifier;II)V")).findFirst().orElseThrow();
            int calls=0;for(var ins:method.instructions)if(ins instanceof org.objectweb.asm.tree.MethodInsnNode call&&call.name.equals("submitModel")){
                assertEquals(org.objectweb.asm.Type.INT_TYPE,org.objectweb.asm.Type.getArgumentTypes(call.desc)[6]);calls++;
            }
            assertEquals(3,calls);
        }
    }
    @Test void armorCanBeTurnedOffIndependently(){
        var module=new HitColor();assertTrue(module.armor());
        ((dev.sqwat.vela.module.setting.BooleanSetting)module.settings().stream().filter(s->s.id().equals("armor")).findFirst().orElseThrow()).set(false);
        assertFalse(module.armor());
    }
    @Test void onlyLivingHurtPlayersGetCustomTint(){
        assertTrue(HitColor.applies(true,true,0));
        assertFalse(HitColor.applies(false,true,0));
        assertFalse(HitColor.applies(true,false,0));
        assertFalse(HitColor.applies(true,true,1));
        assertFalse(new HitColor().enabled());
    }
}
