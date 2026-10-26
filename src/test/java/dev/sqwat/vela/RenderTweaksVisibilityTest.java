package dev.sqwat.vela;
import dev.sqwat.vela.module.RenderTweaks;
import dev.sqwat.vela.module.setting.BooleanSetting;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RenderTweaksVisibilityTest {
    @Test void filtersAreOptInIndependentAndInertWhenDisabled(){
        var module=new RenderTweaks();
        var pictures=(BooleanSetting)module.settings().stream().filter(s->s.id().equals("pictures")).findFirst().orElseThrow();
        assertFalse(module.hides("pictures"));module.setEnabled(true);assertFalse(module.hides("pictures"));
        pictures.set(true);assertTrue(module.hides("pictures"));assertFalse(module.hides("holograms"));
        module.setEnabled(false);assertFalse(module.hides("pictures"));
        module.setEnabled(true);assertTrue(module.hides("pictures"));assertFalse(module.hides("unknown"));
    }
}
