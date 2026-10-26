package dev.sqwat.vela;

import dev.sqwat.vela.ui.animation.Motion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MotionTest {
    @Test
    void approachIsStableAndNeverOvershoots() {
        float value = 0;
        for (int i = 0; i < 240; i++) {
            float previous = value;
            value = Motion.approach(value, 1, 16, 1f / 240f);
            assertTrue(value >= previous && value <= 1);
        }
        assertTrue(value > 0.99f);
    }

    @Test
    void cubicEasingClampsInput() {
        assertEquals(0, Motion.easeOutCubic(-4));
        assertEquals(1, Motion.easeOutCubic(4));
    }
}
