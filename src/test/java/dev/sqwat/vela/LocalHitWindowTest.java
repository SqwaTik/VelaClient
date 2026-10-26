package dev.sqwat.vela;
import dev.sqwat.vela.render.effect.LocalHitWindow;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LocalHitWindowTest {
    @Test void spamCannotRepeatDamage(){
        assertEquals(0,LocalHitWindow.damage(2,7,true));
        assertEquals(0,LocalHitWindow.damage(7,7,true));
    }
    @Test void strongerHitOnlyAddsDifference(){assertEquals(2,LocalHitWindow.damage(9,7,true));}
    @Test void expiredWindowAllowsFullDamage(){assertEquals(7,LocalHitWindow.damage(7,7,false));}
}
