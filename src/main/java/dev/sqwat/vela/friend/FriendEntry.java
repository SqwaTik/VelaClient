package dev.sqwat.vela.friend;

import java.util.UUID;

public record FriendEntry(UUID id, String name, String alias, String note, boolean favorite, long addedAt) {
    public FriendEntry {
        if (id == null) id = UUID.randomUUID();
        name = name == null ? "" : name.strip();
        alias = alias == null ? "" : alias.strip();
        note = note == null ? "" : note.strip();
    }
}
