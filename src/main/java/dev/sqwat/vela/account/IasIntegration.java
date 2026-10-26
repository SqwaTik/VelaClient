package dev.sqwat.vela.account;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import dev.sqwat.vela.ui.screen.VelaMicrosoftLoginScreen;
import dev.sqwat.vela.ui.screen.VelaSessionLoginScreen;
import ru.vidtu.ias.account.MicrosoftAccount;
import ru.vidtu.ias.auth.LoginData;
import ru.vidtu.ias.auth.handlers.LoginHandler;
import ru.vidtu.ias.screen.VelaIasBridge;

import java.util.List;
import java.util.UUID;

/**
 * Small boundary around IAS. Vela owns every account-management screen; IAS is
 * used only for its authenticated Microsoft flow, encrypted storage and the
 * low-level Minecraft session replacement.
 */
public final class IasIntegration {
    private IasIntegration() {}

    public static boolean available() {
        return FabricLoader.getInstance().isModLoaded("ias");
    }

    private static void ensureReady() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() != null || client.level != null)
            throw new IllegalStateException("Сначала выйдите в главное меню");
        if (!available()) throw new IllegalStateException("Установите In-Game Account Switcher для Minecraft 26.2");
    }

    public static List<VelaIasBridge.AccountView> accounts() {
        ensureReady();
        return VelaIasBridge.accounts();
    }

    public static boolean disclaimerAccepted() {
        ensureReady();
        return VelaIasBridge.disclaimerAccepted();
    }

    public static void acceptDisclaimer() {
        ensureReady();
        VelaIasBridge.acceptDisclaimer();
    }

    public static void addMicrosoft(Screen parent, Runnable refresh) {
        openMicrosoft(new VelaMicrosoftLoginScreen(parent,refresh));
    }

    public static void openMicrosoft(VelaMicrosoftLoginScreen screen) {
        ensureReady();
        Minecraft.getInstance().gui.setScreen(screen);
    }

    public static void storeMicrosoft(MicrosoftAccount account, Runnable refresh) {
        ensureReady();
        VelaIasBridge.storeMicrosoft(account, refresh);
    }

    public static void addOffline(String name, Runnable refresh) {
        ensureReady();
        VelaIasBridge.addOffline(name, refresh);
    }

    public static void login(Screen parent, UUID uuid) {
        ensureReady();
        Minecraft.getInstance().gui.setScreen(new VelaSessionLoginScreen(parent, uuid));
    }

    public static void startLogin(UUID uuid, LoginHandler handler) {
        ensureReady();
        VelaIasBridge.startLogin(uuid, handler);
    }

    public static java.util.concurrent.CompletableFuture<Void> applySession(LoginData data, boolean changed) {
        ensureReady();
        return VelaIasBridge.applySession(data, changed);
    }

    public static void delete(UUID uuid, Runnable refresh) {
        ensureReady();
        VelaIasBridge.delete(uuid, refresh);
    }
}
