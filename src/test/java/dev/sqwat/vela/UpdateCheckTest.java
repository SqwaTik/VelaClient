package dev.sqwat.vela;

import dev.sqwat.vela.core.UpdateCheck;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateCheckTest {
    @Test
    void comparesVersionsByNumber() {
        assertTrue(UpdateCheck.isNewer("v0.2.2", "0.2.1"));
        assertTrue(UpdateCheck.isNewer("0.2.10", "0.2.9"));
        assertTrue(UpdateCheck.isNewer("0.3", "0.2.9"));
        assertFalse(UpdateCheck.isNewer("0.2.1", "0.2.1"));
        assertFalse(UpdateCheck.isNewer("v0.2.0", "0.2.1"));
        assertFalse(UpdateCheck.isNewer("", "0.2.1"));
    }

    @Test
    void ignoresTheGameVersionSuffix() {
        assertFalse(UpdateCheck.isNewer("v0.2.1", "0.2.1+1.21.11-fabric"));
        assertTrue(UpdateCheck.isNewer("v0.2.2", "0.2.1+1.21.11-neoforge"));
        assertEquals("0.2.1", UpdateCheck.shortVersion("0.2.1+26.3-fabric"));
    }

    @Test
    void releaseBeatsItsPreRelease() {
        assertTrue(UpdateCheck.isNewer("0.3.0", "0.3.0-beta"));
        assertFalse(UpdateCheck.isNewer("0.3.0-beta", "0.3.0"));
    }
}
