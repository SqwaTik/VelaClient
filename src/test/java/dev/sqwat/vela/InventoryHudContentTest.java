package dev.sqwat.vela;
import dev.sqwat.vela.ui.InventoryHudContent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InventoryHudContentTest {
    @Test void ignoresHotbarAndArmor(){
        assertFalse(InventoryHudContent.hasItems(slot->slot<9||slot>=36));
        assertFalse(InventoryHudContent.hasItems(slot->false));
    }
    @Test void includesBothEndsOfDisplayedInventory(){
        assertTrue(InventoryHudContent.hasItems(slot->slot==9));
        assertTrue(InventoryHudContent.hasItems(slot->slot==35));
    }
}
