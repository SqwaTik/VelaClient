package dev.sqwat.vela;
import dev.sqwat.vela.ui.hud.HudSnap;
import dev.sqwat.vela.module.visual.TargetESP;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EditorSnapTest {
    @Test void sameTwelvePixelMagnetAtDifferentProjectionScales(){
        for(double units:new double[]{.001,.002,.01}){
            assertEquals(0,HudSnap.origin(11*units,0,units,false),1e-12);
            assertEquals(0,HudSnap.origin(-11*units,0,units,false),1e-12);
            assertEquals(13*units,HudSnap.origin(13*units,0,units,false),1e-12);
        }
    }
    @Test void altBypassesAndDefaultHudCoordinatesKeepThreshold(){
        assertEquals(.01,HudSnap.origin(.01,0,.002,true));
        assertEquals(100,HudSnap.origin(111,100,1,false));
        assertEquals(113,HudSnap.origin(113,100,1,false));
    }
    @Test void targetHighlightFadesAndExpires(){
        assertEquals(0,TargetESP.opacity(0,0,500_000_000));
        assertEquals(1,TargetESP.opacity(200_000_000,0,500_000_000));
        assertEquals(.5,TargetESP.opacity(350_000_000,0,500_000_000));
        assertEquals(0,TargetESP.opacity(600_000_000,0,500_000_000));
    }
}
