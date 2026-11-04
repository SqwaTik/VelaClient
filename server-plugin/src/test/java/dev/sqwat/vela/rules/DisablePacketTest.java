package dev.sqwat.vela.rules;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class DisablePacketTest {
    @Test void encodesCountThenLengthPrefixedStrings() {
        assertArrayEquals(new byte[]{2, 8, 'f', 'r', 'e', 'e', 'l', 'o', 'o', 'k', 1, '*'}, DisablePacket.encode(List.of("freelook", "*")));
        assertArrayEquals(new byte[]{0}, DisablePacket.encode(List.of()));
    }

    @Test void countsPastOneByteUseVarInts() {
        byte[] packet = DisablePacket.encode(Collections.nCopies(200, "a"));
        assertEquals((byte) 0xC8, packet[0]);
        assertEquals(1, packet[1]);
        assertEquals(2 + 200 * 2, packet.length);
    }

    @Test void rejectsWhatVelaWouldRefuse() {
        assertThrows(IllegalArgumentException.class, () -> DisablePacket.encode(Collections.nCopies(257, "a")));
        assertThrows(IllegalArgumentException.class, () -> DisablePacket.encode(List.of("a".repeat(65))));
    }
}
