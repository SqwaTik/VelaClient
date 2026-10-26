package dev.sqwat.vela.account;

import com.google.gson.reflect.TypeToken;
import dev.sqwat.vela.config.JsonFileStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Stores profile metadata only. Passwords and access/refresh tokens never enter this file. */
public final class AccountService {
    private final JsonFileStore<List<AccountProfile>> store;
    private final List<AccountProfile> accounts;
    private UUID selected;

    public AccountService(Path root) {
        store = new JsonFileStore<>(root.resolve("accounts.json"), new TypeToken<List<AccountProfile>>() {}.getType(), ArrayList::new);
        accounts = new ArrayList<>(store.load());
    }

    public synchronized void registerLauncherAccount(String username, UUID minecraftId) {
        accounts.removeIf(profile -> profile.provider() == AccountProfile.Provider.LAUNCHER);
        AccountProfile current = new AccountProfile(UUID.randomUUID(), username, minecraftId, AccountProfile.Provider.LAUNCHER, System.currentTimeMillis(), true);
        accounts.add(current);
        selected = current.id();
        save();
    }

    public synchronized List<AccountProfile> all() {
        return accounts.stream().sorted(Comparator.comparing(AccountProfile::current).reversed()
            .thenComparing(Comparator.comparingLong(AccountProfile::lastUsed).reversed())).toList();
    }

    public synchronized UUID selectedId() { return selected; }

    public synchronized void addAuthenticated(AccountProfile profile) {
        accounts.removeIf(existing -> existing.minecraftId() != null && existing.minecraftId().equals(profile.minecraftId()));
        accounts.add(profile);
        save();
    }

    public synchronized ActivationResult requestActivation(UUID profileId, boolean connectedToServer) {
        AccountProfile profile = accounts.stream().filter(account -> account.id().equals(profileId)).findFirst().orElse(null);
        if (profile == null) return new ActivationResult(false, "Аккаунт не найден");
        if (connectedToServer) return new ActivationResult(false, "Сначала отключитесь от сервера");
        if (!profile.current()) return new ActivationResult(false, "Для смены сессии откройте In-Game Account Switcher");
        selected = profileId;
        return new ActivationResult(true, "Аккаунт уже активен");
    }

    public synchronized boolean remove(UUID profileId) {
        boolean changed = accounts.removeIf(profile -> profile.id().equals(profileId) && profile.provider() != AccountProfile.Provider.LAUNCHER);
        if (changed) save();
        return changed;
    }

    private void save() {
        try { store.save(accounts); } catch (IOException ignored) { }
    }

    public record ActivationResult(boolean accepted, String message) {}
}
