package dev.sqwat.vela;
import dev.sqwat.vela.ui.render.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ScrollbarRevealTest {
    @Test void expandsAroundItsCenterWithoutOvershooting(){
        Rect source=new Rect(30,20,2,60);
        for(int frame=0;frame<=100;frame++){
            Rect r=ControlChrome.verticalReveal(source,frame/100f);
            assertTrue(r.y()>=20);assertTrue(r.y()+r.height()<=80);
            assertEquals(50,r.y()+r.height()/2f,.5f);
        }
        assertEquals(0,ControlChrome.verticalReveal(source,0).height());
        assertEquals(source,ControlChrome.verticalReveal(source,1));
    }
}
