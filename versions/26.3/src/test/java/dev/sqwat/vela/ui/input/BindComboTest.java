package dev.sqwat.vela.ui.input;

import dev.sqwat.vela.compat.GLFW;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 26.3 version: SDL scancodes, and key events carry SDL modifier masks ({@link BindInput#combo}). */
class BindComboTest {
    private static final int F=9,LCTRL=224;
    @Test void plainKeysKeepTheirOldValues(){
        assertEquals(F,BindInput.comboBits(F,0));
        assertEquals(-1,BindInput.comboBits(-1,BindInput.CTRL));
        assertEquals(LCTRL,BindInput.comboBits(LCTRL,BindInput.CTRL)); // a modifier alone has no modifier bits
    }
    @Test void combinationsRoundTripAndLabel(){
        int ctrlF=BindInput.combo(F,GLFW.GLFW_MOD_CONTROL);
        assertEquals(F,BindInput.base(ctrlF));
        assertEquals(BindInput.CTRL,BindInput.modifiers(ctrlF));
        assertEquals("Ctrl+F",KeyLabels.of(ctrlF,""));
        assertEquals("Ctrl+Shift+M4",KeyLabels.of(BindInput.comboBits(BindInput.mouse(4),BindInput.CTRL|BindInput.SHIFT),""));
        assertTrue(BindInput.isMouse(BindInput.comboBits(BindInput.mouse(4),BindInput.ALT)));
    }
    @Test void eventsMatchWithTheRequiredModifiers(){
        int ctrlF=BindInput.comboBits(F,BindInput.CTRL);
        assertTrue(BindInput.matches(ctrlF,F,GLFW.GLFW_MOD_CONTROL));
        assertTrue(BindInput.matches(ctrlF,F,GLFW.GLFW_MOD_CONTROL|GLFW.GLFW_MOD_SHIFT));
        assertFalse(BindInput.matches(ctrlF,F,0));
        assertTrue(BindInput.matches(F,F,GLFW.GLFW_MOD_CONTROL));
    }
    @Test void keySettingAcceptsCombinations(){
        var setting=new dev.sqwat.vela.module.setting.KeySetting("k","K",-1);
        setting.set(BindInput.comboBits(F,BindInput.CTRL|BindInput.ALT));
        assertEquals("Ctrl+Alt+F",KeyLabels.of(setting.value(),""));
    }
}
