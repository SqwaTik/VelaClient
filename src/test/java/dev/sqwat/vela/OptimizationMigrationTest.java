package dev.sqwat.vela;

import dev.sqwat.vela.core.*;
import dev.sqwat.vela.core.VelaServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class OptimizationMigrationTest {
    @TempDir Path root;
    @Test void legacyDisabledModuleMigratesOptionsWithoutBecomingBindable() throws Exception {
        var old=new ClientConfig();
        var state=new ClientConfig.ModuleState();
        state.enabled=false;state.boundKey=71;
        state.settings.put("entity_lod_distance",new com.google.gson.JsonPrimitive(52));
        old.modules.put("zako_optimization",state);
        old.favorites.add("zako_optimization");
        new JsonFileStore<>(root.resolve("profiles/default.vela"),ClientConfig.class,ClientConfig::new).save(old);
        try(var services=new VelaServices(root)){
            assertNull(services.modules().byId("zako_optimization"));
            assertTrue(services.zakoOptimization().available());
            assertEquals(52,services.zakoOptimization().entityDistance.value().intValue());
            assertFalse(services.configs().current().favorites.contains("zako_optimization"));
            services.configs().selectProfile("fresh");
            assertEquals(35,services.zakoOptimization().entityDistance.value().intValue());
            services.configs().selectProfile("default");
            assertEquals(52,services.zakoOptimization().entityDistance.value().intValue());
        }
        try(var services=new VelaServices(root)){
            assertEquals(52,services.zakoOptimization().entityDistance.value().intValue());
            assertEquals(52,services.configs().current().optimization.get("entity_lod_distance").getAsInt());
            assertEquals(71,services.configs().current().modules.get("zako_optimization").boundKey);
        }
    }
}
