/*
 * In-Game Account Switcher is a third-party mod for Minecraft Java Edition that
 * allows you to change your logged in account in-game, without restarting it.
 *
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

//? if fabric {
package ru.vidtu.ias.platform;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NullMarked;
import ru.vidtu.ias.IAS;
import ru.vidtu.ias.IASMinecraft;

/**
 * Main IAS class for Fabric.
 *
 * @author VidTu
 * @apiNote Internal use only
 * @see IAS
 */
@ApiStatus.Internal
@NullMarked
public final class IFabric implements ClientModInitializer {
    /**
     * Logger for this class.
     */
    private static final Logger LOGGER = LogManager.getLogger("IAS/IFabric");

    /**
     * Creates a new mod.
     *
     * @apiNote Do not call, called by Fabric
     */
    @Contract(pure = true)
    public IFabric() {
        // Empty.
    }

    /**
     * Initializes the client.
     *
     * @apiNote Do not call, called by Fabric
     */
    @Override
    public void onInitializeClient() {
        // Log.
        long start = System.nanoTime();
        LOGGER.info("IAS: Loading... (platform: fabric)");

        // Init the mod.
        IASMinecraft.init();

        // Register the shutdown handler.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> IAS.close());

        // Vela: account screens are Vela's own, so IAS adds no buttons or title text.
        // Done.
        LOGGER.info("IAS: Loaded. ({} ms)", (System.nanoTime() - start) / 1_000_000L);
    }

    @Contract(pure = true)
    @Override
    public String toString() {
        return "IAS/IFabric{}";
    }
}
//?}
