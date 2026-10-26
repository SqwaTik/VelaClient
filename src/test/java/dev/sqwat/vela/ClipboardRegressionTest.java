package dev.sqwat.vela;
import dev.sqwat.vela.ui.input.TextEdit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ClipboardRegressionTest {
    @Test void pasteDoesNotDeleteLettersFromNamesOrProfileKeys(){
        assertEquals("PlayerConfig_Cntrl123",TextEdit.cleanPaste("Player\nConfig_Cntrl123\t"));
        assertEquals("Привет 🛰",TextEdit.cleanPaste("Привет 🛰"));
    }
}
