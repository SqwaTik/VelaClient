/*
 * In-Game Account Switcher is a mod for Minecraft that allows you to change your logged in account in-game, without restarting Minecraft.
 * Copyright (C) 2015-2022 The_Fireplace
 * Copyright (C) 2021-2026 VidTu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

package ru.vidtu.ias;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
/*import net.minecraft.client.gui.GuiGraphics;*/
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.social.PlayerSocialManager;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vidtu.ias.auth.LoginData;
import ru.vidtu.ias.config.IASConfig;
import ru.vidtu.ias.extension.MinecraftExtension;
import ru.vidtu.ias.mixins.MinecraftAccessor;
import ru.vidtu.ias.platform.IStonecutter;
import ru.vidtu.ias.utils.Expression;
import ru.vidtu.ias.utils.IUtils;
import ru.vidtu.ias.utils.exceptions.FriendlyException;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

//? if >=26.2 {
import com.mojang.authlib.yggdrasil.FriendsService;
import net.minecraft.client.gui.screens.social.RemoteFriendListUpdateHandler;
//?}

/**
 * Main IAS class for Minecraft.
 *
 * @author VidTu
 */
public final class IASMinecraft {
    /**
     * Logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger("IAS/IASMinecraft");

    /**
     * An instance of this class cannot be created.
     *
     * @throws AssertionError Always
     */
    private IASMinecraft() {
        throw new AssertionError("No instances.");
    }

    /**
     * Initializes the IAS.
     */
    public static void init() {
        // Initialize the IAS.
        IAS.init(IStonecutter.GAME_DIRECTORY, IStonecutter.CONFIG_DIRECTORY);
    }

    /**
     * Logins into the minecraft.
     * Can be called from any thread.
     *
     * @param minecraft Minecraft instance
     * @param data      Login data
     * @return Future for logging in
     */
    public static CompletableFuture<Void> account(Minecraft minecraft, LoginData data) {
        // Check if not in-game.
        LOGGER.info("IAS: Received login request: {}", data);
        if (minecraft.player != null || minecraft.level != null || minecraft.getConnection() != null ||
                minecraft.getCameraEntity() != null || minecraft.gameMode != null || minecraft.getSingleplayerServer() != null) {
            return CompletableFuture.failedFuture(new FriendlyException("Changing accounts in world.", "ias.error.world"));
        }

        // Create everything async, because it lags.
        return CompletableFuture.runAsync(() -> {
            // Create the user.
            LOGGER.info("IAS: Creating user...");
            // I have no idea what are the OPTIONAL fields and the game
            // works FINE without them, even with chat reporting and parental control, etc.
            // etc., it may be some telemetry, it may be something else. If something is broken by this
            // feel free to submit an issue, if someone knows what this is, feel free to PR a fix,
            // I'm too lazy to fix anything related to telemetry or chat signatures/reports.
            boolean online = data.online();
            //? if >=1.21.10 {
            User user = new User(data.name(), data.uuid(), data.token(), Optional.empty(), Optional.empty());
            //?} else
            /*User user = new User(data.name(), data.uuid(), data.token(), Optional.empty(), Optional.empty(), online ? User.Type.MSA : User.Type.LEGACY);*/

            // Create various services.
            //? if >=1.21.10 {
            YggdrasilAuthenticationService service = online ? new YggdrasilAuthenticationService(minecraft.getProxy()) : YggdrasilAuthenticationService.createOffline(minecraft.getProxy());
            Services services = Services.create(service, minecraft.gameDirectory);
            CompletableFuture<ProfileResult> profile = CompletableFuture.completedFuture(online ? services.sessionService().fetchProfile(data.uuid(), true) : null);
            //?} else {
            /*YggdrasilAuthenticationService service = new YggdrasilAuthenticationService(minecraft.getProxy());
            CompletableFuture<ProfileResult> profile = CompletableFuture.completedFuture(online ? minecraft.getMinecraftSessionService().fetchProfile(data.uuid(), true) : null);*/
            //?}
            @SuppressWarnings("CastToIncompatibleInterface") // <- Mixin Accessor.
            MinecraftAccessor accessor = (MinecraftAccessor) minecraft;
            final GameConfig originalConfig = ((MinecraftExtension) minecraft).ias_gameConfig();
            //? if >=1.21.10 {
            final GameConfig config = new GameConfig(new GameConfig.UserData(user, minecraft.getProxy()), originalConfig.display, originalConfig.location, originalConfig.game, originalConfig.quickPlay);
            //?} else {
            /*final GameConfig config = new GameConfig(new GameConfig.UserData(user, originalConfig.user.userProperties, originalConfig.user.profileProperties, minecraft.getProxy()), originalConfig.display, originalConfig.location, originalConfig.game, originalConfig.quickPlay);
            *///?}
            //? if >=26.2 {
            UserApiService apiService = online ? MinecraftAccessor.ias$createUserApiService(service, config) : UserApiService.OFFLINE;
            //?} else {
            /*UserApiService apiService = online ? accessor.ias$createUserApiService(service, config) : UserApiService.OFFLINE;
            *///?}
            UserApiService.UserProperties properties;
            try {
                properties = apiService.fetchProperties();
            } catch (Throwable ignored) {
                properties = UserApiService.OFFLINE_PROPERTIES;
            }
            CompletableFuture<UserApiService.UserProperties> propertiesFuture = CompletableFuture.completedFuture(properties);
            //? if >= 26.2 {
            FriendsService friends = service.createFriendsService(data.token());
            RemoteFriendListUpdateHandler friendList = new RemoteFriendListUpdateHandler(friends, minecraft);
            PlayerSocialManager social = new PlayerSocialManager(minecraft, apiService, friends, friendList);
            //?} else {
            /*PlayerSocialManager social = new PlayerSocialManager(minecraft, apiService);
            *///?}
            ClientTelemetryManager telemetry = new ClientTelemetryManager(minecraft, apiService, user);
            ProfileKeyPairManager keyPair = ProfileKeyPairManager.create(apiService, user, minecraft.gameDirectory.toPath());
            ReportingContext reporting = ReportingContext.create(ReportEnvironment.local(), apiService);

            // Schedule to the main thread
            minecraft.execute(() -> {
                // Flush everything.
                LOGGER.info("IAS: Flushing user...");
                //? if >=1.21.10 {
                accessor.ias$services(services);
                //?}
                accessor.ias$user(user);
                accessor.ias$profileFuture(profile);
                accessor.ias$userApiService(apiService);
                accessor.ias$userPropertiesFuture(propertiesFuture);
                accessor.ias$playerSocialManager(social);
                //? if >=26.2 {
                accessor.ias$remoteFriendListUpdateHandler().close();
                accessor.ias$remoteFriendListUpdateHandler(friendList);
                if (social.isFriendListEnabled()) {
                    friendList.start();
                }
                //?}
                accessor.ias$telemetryManager(telemetry);
                accessor.ias$profileKeyPairManager(keyPair);
                accessor.ias$reportingContext(reporting);
                minecraft.updateTitle();
                LOGGER.info("IAS: Flushed user.");
            });
        }, IAS.executor()).exceptionally(t -> {
            // Log it.
            LOGGER.error("IAS: Unable to log in: {}.", data, t);

            // Rethrow.
            throw new RuntimeException("Unable to change account to: " + data, t);
        });
    }
}
