package dev.sqwat.vela;
import dev.sqwat.vela.ui.BindInput;
import dev.sqwat.vela.ui.KeyLabels;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BindInputTest {
    @Test void keypadAndSystemKeysNeverUseStaleFallback(){
        for(int key=320;key<=329;key++)assertEquals("Num "+(key-320),KeyLabels.of(key,"None"));
        assertEquals("Num Enter",KeyLabels.of(335,"None"));
        assertEquals("Num Lock",KeyLabels.of(282,"key50"));
        assertEquals("RWin",KeyLabels.of(347,"None"));
        assertEquals("Menu",KeyLabels.of(348,"None"));
    }
    @Test void allMouseButtonsRoundTrip(){for(int i=0;i<8;i++){int key=BindInput.mouse(i);assertTrue(BindInput.isMouse(key));assertEquals(i,BindInput.mouseButton(key));assertEquals("M"+(i+1),KeyLabels.of(key,""));}}
    @Test void escapeAndDeleteClear(){assertTrue(BindInput.clears(256));assertTrue(BindInput.clears(261));assertFalse(BindInput.clears(65));assertFalse(BindInput.isMouse(-1));}
}
