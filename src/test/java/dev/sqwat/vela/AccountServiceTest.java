package dev.sqwat.vela;

import dev.sqwat.vela.account.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {
    @TempDir Path directory;

    @Test
    void metadataFileNeverContainsTokenOrPasswordFields() throws Exception {
        AccountService service = new AccountService(directory);
        service.registerLauncherAccount("TestUser", UUID.randomUUID());
        String json = Files.readString(directory.resolve("accounts.json")).toLowerCase();

        assertFalse(json.contains("accesstoken"));
        assertFalse(json.contains("refresh"));
        assertFalse(json.contains("password"));
        assertTrue(json.contains("testuser"));
    }
}
