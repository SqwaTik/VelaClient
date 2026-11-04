package dev.sqwat.vela.neoforge.shim.api;

/** Fabric's client entrypoint; NeoMod calls each one. */
public interface ClientModInitializer {
    void onInitializeClient();
}
