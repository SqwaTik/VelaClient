package dev.sqwat.vela.ui.hud;
import java.util.function.IntPredicate;
public final class InventoryHudContent {
    private InventoryHudContent() {}
    public static boolean hasItems(IntPredicate occupied) {
        for(int slot=9;slot<36;slot++)if(occupied.test(slot))return true;
        return false;
    }
}
