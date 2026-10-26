package dev.sqwat.vela;
import dev.sqwat.vela.ui.animation.LoadingProgress;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LoadingProgressTest {
    @Test void neverMovesBackDuringReloadPhases(){var p=new LoadingProgress();float last=0;for(int i=0;i<100;i++){float next=p.update(i<30?.8f:.2f,false,.016f);assertTrue(next>=last);assertTrue(next<=.8f);last=next;}}
    @Test void avoidsJumpAndConvergesOnCompletion(){var p=new LoadingProgress();assertTrue(p.update(1,false,.016f)<.02f);float value=0;for(int i=0;i<180;i++)value=p.update(1,true,1/60f);assertEquals(1,value,.001f);}
    @Test void comparableAcrossFrameRates(){assertEquals(simulate(30),simulate(240),.015f);}
    private float simulate(int fps){var p=new LoadingProgress();float v=0;for(int i=0;i<fps;i++)v=p.update(.7f,false,1f/fps);return v;}
}
