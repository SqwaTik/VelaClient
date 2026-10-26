package dev.sqwat.vela;
import dev.sqwat.vela.ui.hud.HudVisibility;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BackgroundReloadHudTest {
    @Test void backgroundReloadPreservesHudAndBlockingOverlaysHideIt(){
        assertTrue(HudVisibility.allowed(true,false,false,false,true));
        assertTrue(HudVisibility.allowed(true,true,true,false,true));
        assertTrue(HudVisibility.allowed(false,true,false,false,false));
        assertFalse(HudVisibility.allowed(true,false,false,false,false));
    }
}
