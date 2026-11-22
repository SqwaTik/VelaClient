package dev.sqwat.vela.neoforge.shim.loader.api;

import dev.sqwat.vela.opt.platform.NeoPlatform;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Fabric Loader's questions answered from NeoForge's loading mod list. */
public final class FabricLoader {
    private static final FabricLoader INSTANCE = new FabricLoader();

    private FabricLoader() {}

    public static FabricLoader getInstance() { return INSTANCE; }

    public Path getConfigDir() { return FMLPaths.CONFIGDIR.get(); }

    public Path getGameDir() { return FMLPaths.GAMEDIR.get(); }

    public boolean isModLoaded(String id) { return NeoPlatform.mod(id).isPresent(); }

    public boolean isDevelopmentEnvironment() { return !FMLLoader.getCurrent().isProduction(); }

    public Optional<ModContainer> getModContainer(String id) {
        return NeoPlatform.mod(id).map(mod -> new ModContainer(mod));
    }

    /** Fabric entrypoints (Bobby's frex_flawless_frames) have no NeoForge counterpart. */
    public <T> List<T> getEntrypoints(String key, Class<T> type) { return List.of(); }
}
