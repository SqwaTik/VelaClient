package dev.sqwat.vela.account;

import java.util.UUID;

public record AccountProfile(UUID id, String username, UUID minecraftId, Provider provider, long lastUsed, boolean current) {
    public AccountProfile {
        if (id == null) id = UUID.randomUUID();
        username = username == null || username.isBlank() ? "Unknown" : username.strip();
        provider = provider == null ? Provider.MICROSOFT : provider;
    }

    public enum Provider {
        LAUNCHER,
        MICROSOFT
    }
}
