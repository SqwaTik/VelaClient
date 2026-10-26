package dev.sqwat.vela;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModuleRegistryTest {
    @Test
    void filtersByCategoryAndLocalizedText() {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(new TestModule("sky", "Custom Sky", "Атмосфера неба", ModuleCategory.VISUALS));
        registry.register(new TestModule("fps", "FPS", "Счётчик", ModuleCategory.HUD));

        assertEquals(1, registry.find(ModuleCategory.VISUALS, "неба").size());
        assertEquals("sky", registry.find(ModuleCategory.VISUALS, "custom").getFirst().id());
        assertTrue(registry.find(ModuleCategory.HUD, "sky").isEmpty());
    }

    @Test
    void rejectsDuplicateStableIds() {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(new TestModule("same", "One", "", ModuleCategory.HUD));
        assertThrows(IllegalArgumentException.class, () -> registry.register(new TestModule("same", "Two", "", ModuleCategory.HUD)));
    }

    private static final class TestModule extends ClientModule {
        private TestModule(String id, String name, String description, ModuleCategory category) { super(id, name, description, category); }
    }
}
