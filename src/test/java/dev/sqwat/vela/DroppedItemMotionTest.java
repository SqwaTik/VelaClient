package dev.sqwat.vela;
import dev.sqwat.vela.render.DroppedItemMotion;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DroppedItemMotionTest {
    @Test void cubesKeepLandingAngleAndWaterSlowsRotation(){var dry=new DroppedItemMotion();var wet=new DroppedItemMotion();dry.sample(0,false,false,true,1);wet.sample(0,false,true,true,1);float a=dry.sample(1,false,false,true,1);float b=wet.sample(1,false,true,true,1);assertTrue(b>0&&b<a);assertEquals(a,dry.sample(2,true,false,true,1));}
    @Test void repeatedFramesDoNotAdvancePhysics(){var p=new DroppedItemMotion();p.sample(0,false,0);float a=p.sample(1,false,.5f);assertEquals(a,p.sample(1,false,.5f));}
    @Test void landingSettlesSmoothlyOnFlatSide(){var p=new DroppedItemMotion();for(int i=0;i<7;i++)p.sample(i,false,1);float air=p.sample(6,false,1);float first=p.sample(7,true,1);assertTrue(Math.abs(first-air)<20);for(int i=8;i<60;i++)p.sample(i,true,1);assertEquals(90,p.sample(60,true,1),.001);}
    @Test void rotatedBoundingBoxRestsAboveGround(){for(int a=0;a<360;a+=5){double lift=DroppedItemMotion.floorOffset(-.4,.3,-.1,.2,a);for(double y:new double[]{-.4,.3})for(double z:new double[]{-.1,.2})assertTrue(lift+y*Math.cos(Math.toRadians(a))-z*Math.sin(Math.toRadians(a))>=-1e-8);}}
}
