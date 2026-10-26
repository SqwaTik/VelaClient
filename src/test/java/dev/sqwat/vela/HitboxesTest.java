package dev.sqwat.vela;
import dev.sqwat.vela.module.Hitboxes;
import dev.sqwat.vela.module.setting.BooleanSetting;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HitboxesTest {
    @Test void selfIsHiddenByDefaultAndAlwaysHiddenInFirstPerson(){
        var m=new Hitboxes();assertFalse(m.selected(true,false,false));assertTrue(m.selected(false,true,true));
        ((BooleanSetting)m.settings().stream().filter(s->s.id().equals("self")).findFirst().orElseThrow()).set(true);
        assertTrue(m.selected(true,false,false));assertFalse(m.selected(true,false,true));
    }
    @Test void interpolationOnlyTranslatesTheRealBox(){
        var original=new AABB(10,2,3,10.6,3.8,3.6);
        var rendered=Hitboxes.renderedBox(original,new Vec3(10,2,3),new Vec3(9.5,2,3));
        assertEquals(9.5,rendered.minX,.00001);assertEquals(original.getXsize(),rendered.getXsize(),.00001);
        assertEquals(original.minY,rendered.minY);assertEquals(original.maxZ,rendered.maxZ);
    }
}
