package dev.sqwat.vela.ui.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BindComboTest {
    private static final int F=70,LCTRL=341;
    @Test void plainKeysKeepTheirOldValues(){
        assertEquals(F,BindInput.combo(F,0));
        assertEquals(-1,BindInput.combo(-1,BindInput.CTRL));
        assertEquals(LCTRL,BindInput.combo(LCTRL,BindInput.CTRL)); // a modifier alone has no modifier bits
    }
    @Test void combinationsRoundTripAndLabel(){
        int ctrlF=BindInput.combo(F,BindInput.CTRL);
        assertEquals(F,BindInput.base(ctrlF));
        assertEquals(BindInput.CTRL,BindInput.modifiers(ctrlF));
        assertEquals("Ctrl+F",KeyLabels.of(ctrlF,""));
        assertEquals("Ctrl+Shift+M4",KeyLabels.of(BindInput.combo(BindInput.mouse(3),BindInput.CTRL|BindInput.SHIFT),""));
        assertTrue(BindInput.isMouse(BindInput.combo(BindInput.mouse(3),BindInput.ALT)));
    }
    @Test void eventsMatchWithTheRequiredModifiers(){
        int ctrlF=BindInput.combo(F,BindInput.CTRL);
        assertTrue(BindInput.matches(ctrlF,F,BindInput.CTRL));
        assertTrue(BindInput.matches(ctrlF,F,BindInput.CTRL|BindInput.SHIFT));
        assertFalse(BindInput.matches(ctrlF,F,0));
        assertTrue(BindInput.matches(F,F,BindInput.CTRL));
    }
    @Test void keySettingAcceptsCombinations(){
        var setting=new dev.sqwat.vela.module.setting.KeySetting("k","K",-1);
        setting.set(BindInput.combo(F,BindInput.CTRL|BindInput.ALT));
        assertEquals("Ctrl+Alt+F",KeyLabels.of(setting.value(),""));
    }
}
