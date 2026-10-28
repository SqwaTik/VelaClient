package dev.sqwat.vela;

import dev.sqwat.vela.core.*;
import dev.sqwat.vela.core.VelaServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Configs from versions with the bundled Zako optimizations: Vela's own switches carry over, Zako's are dropped. */
class OptimizationMigrationTest {
    @TempDir Path root;
    @Test void vela_switches_survive_and_zako_leftovers_are_removed() throws Exception {
        var old=new ClientConfig();
        var state=new ClientConfig.ModuleState();
        state.enabled=false;state.boundKey=71;
        state.settings.put("occlusion_mobs",new com.google.gson.JsonPrimitive(false));
        state.settings.put("entity_lod_distance",new com.google.gson.JsonPrimitive(52));
        old.modules.put("zako_optimization",state);
        old.favorites.add("zako_optimization");
        new JsonFileStore<>(root.resolve("profiles/default.vela"),ClientConfig.class,ClientConfig::new).save(old);
        try(var services=new VelaServices(root)){
            assertNull(services.modules().byId("zako_optimization"));
            assertFalse(services.optimization().occludeMobs());
            assertTrue(services.optimization().occludePlayers());
            assertFalse(services.configs().current().favorites.contains("zako_optimization"));
            assertFalse(services.configs().current().modules.containsKey("zako_optimization"));
            assertFalse(services.configs().current().optimization.containsKey("entity_lod_distance"));
            services.configs().selectProfile("fresh");
            assertTrue(services.optimization().occludeMobs());
            services.configs().selectProfile("default");
            assertFalse(services.optimization().occludeMobs());
        }
        try(var services=new VelaServices(root)){
            assertFalse(services.optimization().occludeMobs());
        }
    }
}
