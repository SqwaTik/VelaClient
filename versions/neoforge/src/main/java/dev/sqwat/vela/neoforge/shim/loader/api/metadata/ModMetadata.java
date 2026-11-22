package dev.sqwat.vela.neoforge.shim.loader.api.metadata;

import dev.sqwat.vela.neoforge.shim.loader.api.Version;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

public final class ModMetadata {
    private final ModInfo mod;

    public ModMetadata(ModInfo mod) { this.mod = mod; }

    public String getId() { return mod.getModId(); }

    public String getName() { return mod.getDisplayName(); }

    public String getDescription() { return mod.getDescription(); }

    public Version getVersion() { return new Version(mod.getVersion().toString()); }
}
