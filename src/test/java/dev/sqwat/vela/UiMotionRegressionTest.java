package dev.sqwat.vela;
import dev.sqwat.vela.ui.animation.FiniteMotion;
import dev.sqwat.vela.render.resource.ReloadWorldBinding;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UiMotionRegressionTest {
 @Test void sharedScrollHasSameResponseAtDifferentFrameRates(){
  float baseline=0;
  for(int fps:new int[]{30,60,180}){
   float value=0;
   for(int frame=0;frame<fps/2;frame++)value=dev.sqwat.vela.ui.animation.Motion.scroll(value,100,1f/fps);
   assertTrue(value>99 && value<=100);
   if(baseline>0)assertEquals(baseline,value,.001f);
   baseline=value;
  }
 }
 @Test void hoverFinishesWithoutASecondPixelStep(){var m=new FiniteMotion();float previous=0;for(int i=0;i<30;i++){float value=m.update(1,.01f,.2f);assertTrue(value>=previous);previous=value;}assertEquals(1,previous);assertEquals(1,m.update(1,.5f,.2f));}
 @Test void reversalStartsAtCurrentPosition(){var m=new FiniteMotion();m.update(1,.1f,.2f);float v=m.update(0,0,.2f);assertEquals(.5f,v,.001);assertEquals(0,m.update(0,.2f,.2f));}
 @Test void joiningDoesNotExposeReloadNotice(){var b=new ReloadWorldBinding();Object world=new Object();assertFalse(b.visible(world,true,false));assertTrue(b.visible(world,true,true));assertFalse(b.visible(new Object(),true,true));}
}
