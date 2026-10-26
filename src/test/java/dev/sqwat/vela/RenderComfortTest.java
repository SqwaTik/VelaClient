package dev.sqwat.vela;
import dev.sqwat.vela.module.utility.RenderTweaks;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RenderComfortTest {
    @Test void waterAndLavaShareOneClearlyNamedControl(){var m=new RenderTweaks();assertEquals("Лава и вода",m.settings().stream().filter(s->s.id().equals("fluid_fog")).findFirst().orElseThrow().name());}
    @Test void comfortOptionsAreIndependentAndOffByDefault(){var m=new RenderTweaks();for(String id:new String[]{"hurt_camera","fluid_fog"})assertEquals(false,m.settings().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow().value());assertFalse(m.settings().stream().anyMatch(s->s.id().equals("water_fog")||s.id().equals("lava_fog")));}
}
