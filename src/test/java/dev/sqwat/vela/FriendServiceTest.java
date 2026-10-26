package dev.sqwat.vela;

import dev.sqwat.vela.friend.FriendService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FriendServiceTest {
    @TempDir Path directory;

    @Test
    void deduplicatesNamesCaseInsensitivelyAndPersists() {
        FriendService service = new FriendService(directory);
        assertTrue(service.add("PlayerOne"));
        assertFalse(service.add("playerone"));
        assertFalse(service.add(""));

        FriendService restored = new FriendService(directory);
        assertTrue(restored.contains("PLAYERONE"));
        assertEquals(1, restored.all().size());
    }
}
