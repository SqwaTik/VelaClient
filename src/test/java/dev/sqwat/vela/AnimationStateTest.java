package dev.sqwat.vela;
import dev.sqwat.vela.ui.animation.AnimationState;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
class AnimationStateTest {
    @Test void reversingKeepsCurrentValueAndMovesBack(){var t=new AtomicLong();var a=new AnimationState(t::get);a.value("x",0,false);t.set(100_000_000);float middle=a.value("x",1,false);assertEquals(middle,a.value("x",0,false));t.set(120_000_000);float reverse=a.value("x",0,false);assertTrue(reverse>0&&reverse<middle);}
    @Test void checkboxActuallyHasIntermediateFrames(){var t=new AtomicLong();var a=new AnimationState(t::get);assertEquals(0,a.value("check",0,false));t.addAndGet(16_666_667);float first=a.value("check",1,false);assertTrue(first>0&&first<.5f);t.addAndGet(16_666_667);assertTrue(a.value("check",1,false)>first);}
    @Test void reducedMotionIsExplicitAndImmediate(){var t=new AtomicLong();var a=new AnimationState(t::get);a.value("x",0,false);assertEquals(1,a.value("x",1,true));}
    @Test void sameFrameDoesNotAdvanceTwice(){var t=new AtomicLong();var a=new AnimationState(t::get);a.value("x",0,false);t.addAndGet(20_000_000);float v=a.value("x",1,false);assertEquals(v,a.value("x",1,false));}
    @Test void frameRatesReachTheSameValue(){float[] end=new float[3];int[] fps={30,60,144};for(int j=0;j<fps.length;j++){var t=new AtomicLong();var a=new AnimationState(t::get);a.value("x",0,false);for(int i=1;i<=fps[j];i++){t.set(Math.round(i*1e9/fps[j]));end[j]=a.value("x",1,false);}}assertEquals(end[0],end[1],.00001);assertEquals(end[1],end[2],.00001);}
}
