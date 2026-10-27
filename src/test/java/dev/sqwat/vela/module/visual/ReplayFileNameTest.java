package dev.sqwat.vela.module.visual;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReplayFileNameTest {
    @Test void removesCharactersWindowsRejects(){
        String name=ReplayFiles.fileName("Zombie: \"boss\" <a/b\\c>|?*",0);
        assertFalse(name.matches(".*[\\\\/:*?\"<>|].*"),name);
        assertTrue(name.endsWith("Zombie boss abc"),name);
    }
    @Test void emptyTitleKeepsOnlyTheTime(){
        assertEquals(ReplayFiles.fileName("",0),ReplayFiles.fileName(null,0));
        assertTrue(ReplayFiles.fileName("  ",0).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}-\\d{2}-\\d{2}"));
    }
}
