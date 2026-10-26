package dev.sqwat.vela;

import dev.sqwat.vela.ui.hud.PlayerDisplayLabel;
import dev.sqwat.vela.ui.hud.NearPlayers;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerDisplayLabelTest {
    @Test void removesFormattingAndDecorativeBulletsButKeepsRankAndName() {
        assertEquals("VIP MrHaryus", PlayerDisplayLabel.clean("§a●§f VIP §7MrHaryus"));
        assertEquals("Герой Игрок", PlayerDisplayLabel.clean("\uE001 §x§F§F§0§0§A§AГерой  Игрок"));
    }
    @Test void friendsArePrioritizedWithoutExceedingRadiusOrLimit() {
        var entries=List.of(1,2,130,141);
        assertEquals(List.of(130,1),NearPlayers.closest(entries,2,p->true,p->p*p,p->p>=130));
        assertEquals(List.of(1,2),NearPlayers.closest(entries,2,p->true,p->p*p));
        assertEquals(List.of(2),NearPlayers.closest(entries,2,p->p==2,p->p*p,p->true));
    }
}
