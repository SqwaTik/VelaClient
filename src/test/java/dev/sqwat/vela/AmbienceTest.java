package dev.sqwat.vela;
import dev.sqwat.vela.module.Ambience;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AmbienceTest {
    @Test void clockConvertsWallHoursAndWraps(){assertEquals(0,Ambience.clockAtHour(6));assertEquals(6000,Ambience.clockAtHour(12));assertEquals(18000,Ambience.clockAtHour(0));assertEquals(Ambience.clockAtHour(0),Ambience.clockAtHour(24));}
}
