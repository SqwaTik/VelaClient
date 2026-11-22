package dev.sqwat.vela;
import dev.sqwat.vela.ui.input.BindInput;
import dev.sqwat.vela.ui.input.KeyLabels;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/** 26.3 version: keys are SDL scancodes, mouse buttons count from 1 (left). */
class BindInputTest {
    @Test void keypadAndSystemKeysNeverUseStaleFallback(){
        assertEquals("Num 0",KeyLabels.of(98,"None"));
        for(int key=89;key<=97;key++)assertEquals("Num "+(key-88),KeyLabels.of(key,"None"));
        assertEquals("Num Enter",KeyLabels.of(88,"None"));
        assertEquals("Num Lock",KeyLabels.of(83,"key50"));
        assertEquals("RWin",KeyLabels.of(231,"None"));
        assertEquals("Menu",KeyLabels.of(101,"None"));
    }
    @Test void allMouseButtonsRoundTrip(){for(int i=1;i<=8;i++){int key=BindInput.mouse(i);assertTrue(BindInput.isMouse(key));assertEquals(i,BindInput.mouseButton(key));assertEquals("M"+i,KeyLabels.of(key,""));}}
    @Test void escapeAndDeleteClear(){assertTrue(BindInput.clears(41));assertTrue(BindInput.clears(76));assertFalse(BindInput.clears(4));assertFalse(BindInput.isMouse(-1));}
}
