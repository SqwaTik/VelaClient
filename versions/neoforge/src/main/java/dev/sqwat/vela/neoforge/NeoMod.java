package dev.sqwat.vela.neoforge;

import de.johni0702.minecraft.bobby.Bobby;
import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.ui.screen.VelaDashboardScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import ru.vidtu.ias.platform.IFabric;

/** Vela on NeoForge: the Fabric entrypoints (Vela, the bundled Bobby and IAS) run through the shims for their Fabric API calls. */
@Mod(value = VelaClient.MOD_ID, dist = Dist.CLIENT)
public final class NeoMod {
    public NeoMod(IEventBus modBus, ModContainer container) {
        NeoEvents.register(modBus);
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (mod, parent) -> VelaClient.services() == null ? parent : new VelaDashboardScreen(parent, VelaClient.services()));
    }

    /** Called once the game client exists (see ClientStartMixin): the mod is built before it, the entrypoints need it. */
    public static void initializeClient() {
        new VelaClient().onInitializeClient();
        new Bobby().onInitializeClient();
        new IFabric().onInitializeClient();
    }
}
