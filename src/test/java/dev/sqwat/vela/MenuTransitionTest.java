package dev.sqwat.vela;
import dev.sqwat.vela.ui.MenuTransition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MenuTransitionTest {
    @Test void screenTransitionNeverScalesRenderingOrPointerMovement(){
        for(int frame=0;frame<30;frame++){
            assertEquals(1f,MenuTransition.frame(null));
            assertEquals(37.5,MenuTransition.delta(37.5));
        }
    }
}
