package dev.sqwat.vela;
import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.module.utility.ClickPearl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class RetiredModulesTest {
    @TempDir Path root;
    @Test void removedModulesCannotBeLoadedFromRegistry(){
        try(var services=new VelaServices(root)){
            assertSame(services.modules().byId("far"),services.far());
            for(String id:new String[]{"attack_aura","crystal_aura","web_trap","target_pearl","no_web","elytra_swap","gui_move","autoclicker","sorter","krikun"})assertNull(services.modules().byId(id),id);
            assertNotNull(services.modules().byId("click_pearl"));assertNotNull(services.modules().byId("crystal_optimizer"));
        }
    }
    @Test void pearlLookupNeverUsesMainInventory(){
        assertEquals(8,ClickPearl.findHotbarSlot(i->i==8));
        assertEquals(-1,ClickPearl.findHotbarSlot(i->i==9));
        assertEquals(2,ClickPearl.findHotbarSlot(i->i==2||i==5));
    }
}
