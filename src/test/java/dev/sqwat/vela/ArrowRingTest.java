package dev.sqwat.vela;
import dev.sqwat.vela.ui.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ArrowRingTest {
    @Test void crowdedBearingsSeparateOnTheSameCircle(){
        float[] angles={-3.13f,-3.12f,-3.11f,3.12f};
        ArrowRing.spread(angles,4,.2f,new int[4],new float[4]);
        for(int i=0;i<4;i++)for(int j=0;j<i;j++)assertTrue(Math.acos(Math.cos(angles[i]-angles[j]))>=.199);
        assertTrue(ArrowRing.radius(20,60)>ArrowRing.radius(4,60));
    }
    @Test void distantBearingsKeepTheirDirection(){
        float[] angles={-2,0,2};
        ArrowRing.spread(angles,3,.2f,new int[3],new float[3]);
        assertArrayEquals(new float[]{-2,0,2},angles,.0001f);
    }
    @Test void fullCrowdFitsWithoutChangingIconScale(){
        float[] angles=new float[24];
        ArrowRing.spread(angles,24,1,new int[24],new float[24]);
        for(int i=0;i<24;i++)for(int j=0;j<i;j++)assertTrue(Math.acos(Math.cos(angles[i]-angles[j]))>.26);
    }
    @Test void skippedRenderTicksDoNotHideExistingPlayers(){
        var presence=new NearPresence<String>();
        assertFalse(presence.observe("player",1));presence.finish(1);
        assertTrue(presence.observe("player",20));presence.finish(20);
        assertTrue(presence.observe("player",300));presence.finish(300);
        presence.finish(301);
        assertFalse(presence.observe("player",302));
    }
}
