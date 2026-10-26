package ru.vidtu.ias.screen;

import net.minecraft.client.Minecraft;
import ru.vidtu.ias.IAS;
import ru.vidtu.ias.IASMinecraft;
import ru.vidtu.ias.account.Account;
import ru.vidtu.ias.account.OfflineAccount;
import ru.vidtu.ias.auth.LoginData;
import ru.vidtu.ias.auth.handlers.LoginHandler;
import ru.vidtu.ias.config.IASStorage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Package bridge for the intentionally package-private IAS flow screens.
 * Credentials and access tokens never cross this boundary.
 */
public final class VelaIasBridge {
    public record AccountView(UUID uuid, UUID skinUuid, String name, boolean microsoft, boolean insecure) {}

    private VelaIasBridge() {}

    public static List<AccountView> accounts() {
        return IASStorage.ACCOUNTS.stream()
            .map(account -> new AccountView(account.uuid(), account.skin(), account.name(), account.canLogin(), account.insecure()))
            .toList();
    }

    public static boolean disclaimerAccepted() {
        return IASStorage.gameDisclaimerShown;
    }

    public static void acceptDisclaimer() {
        IAS.gameDisclaimerShownStorage();
        IAS.disclaimersStorage();
    }

    public static void storeMicrosoft(Account account, Runnable refresh) {
        if (!account.canLogin()) throw new IllegalArgumentException("Expected a Microsoft account");
        IASStorage.ACCOUNTS.removeIf(existing -> existing.equals(account));
        IASStorage.ACCOUNTS.add(account);
        save();
        if (refresh != null) refresh.run();
    }

    public static void addOffline(String name, Runnable refresh) {
        String normalized = name == null ? "" : name.strip();
        if (!normalized.matches("[A-Za-z0-9_]{3,16}")) {
            throw new IllegalArgumentException("Ник: 3–16 латинских букв, цифр или _");
        }
        Account account = new OfflineAccount(normalized, null);
        IASStorage.ACCOUNTS.removeIf(existing -> existing.equals(account));
        IASStorage.ACCOUNTS.add(account);
        save();
        if (refresh != null) refresh.run();
    }

    public static void startLogin(UUID uuid, LoginHandler handler) {
        Account account = IASStorage.ACCOUNTS.stream()
            .filter(candidate -> candidate.uuid().equals(uuid))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Аккаунт больше не существует"));

        if (account.canLogin()) {
            IAS.executor().execute(() -> account.login(handler, null));
        } else {
            String name = account.name();
            handler.success(new LoginData(name, OfflineAccount.uuid(name), "ias:offline", false), false);
        }
    }

    public static CompletableFuture<Void> applySession(LoginData data, boolean changed) {
        if (changed) save();
        return IASMinecraft.account(Minecraft.getInstance(), data);
    }

    public static void delete(UUID uuid, Runnable refresh) {
        IASStorage.ACCOUNTS.removeIf(account -> account.uuid().equals(uuid));
        save();
        if (refresh != null) refresh.run();
    }

    private static void save() {
        IAS.disclaimersStorage();
        IAS.saveStorage();
    }
}
