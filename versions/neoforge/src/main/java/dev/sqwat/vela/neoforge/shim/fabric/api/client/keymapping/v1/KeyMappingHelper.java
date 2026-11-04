package dev.sqwat.vela.neoforge.shim.fabric.api.client.keymapping.v1;

import net.minecraft.client.KeyMapping;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class KeyMappingHelper {
    /** Handed to NeoForge's RegisterKeyMappingsEvent. */
    public static final List<KeyMapping> PENDING = new CopyOnWriteArrayList<>();

    private KeyMappingHelper() {}

    public static KeyMapping registerKeyMapping(KeyMapping mapping) {
        PENDING.add(mapping);
        return mapping;
    }
}
