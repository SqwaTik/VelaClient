package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.visual.TabPing;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodInfoTest {
    @Test void lineShowsHungerAndSaturation(){
        assertEquals("Сытость +6  ·  Насыщение +7.2",FoodInfo.line(6,7.2f).getString());
        assertEquals("Сытость +4  ·  Насыщение +2",FoodInfo.line(4,2f).getString());
    }
    @Test void pingColoursByLatency(){
        assertNotEquals(TabPing.color(40),TabPing.color(400));
        assertEquals(TabPing.color(0),TabPing.color(-1));
    }
}
