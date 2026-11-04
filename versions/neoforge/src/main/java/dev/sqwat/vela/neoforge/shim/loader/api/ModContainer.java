package dev.sqwat.vela.neoforge.shim.loader.api;

import dev.sqwat.vela.neoforge.shim.loader.api.metadata.ModMetadata;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

public final class ModContainer {
    private final ModMetadata metadata;

    ModContainer(ModInfo mod) { metadata = new ModMetadata(mod); }

    public ModMetadata getMetadata() { return metadata; }
}
