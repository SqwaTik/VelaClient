package dev.sqwat.vela;
import dev.sqwat.vela.ui.ArrowLayout;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ArrowLayoutTest {
    @Test void coincidentPlayersGetSeparatePositionsOnOneCircle(){
        var angles=new float[24];var layout=new ArrowLayout(24);
        var placed=layout.place(angles,24,130,28);
        for(int i=0;i<24;i++)for(int j=0;j<i;j++){
            double distance=260*Math.abs(Math.sin((placed[i]-placed[j])/2));
            assertTrue(distance>=27.99,"Overlapping arrows");
        }
    }
    @Test void separatedDirectionsRemainUnchanged(){
        float[] a={0,1,2,3,-2,-1};var p=new ArrowLayout(24).place(a,a.length,100,24);
        for(int i=0;i<a.length;i++)assertEquals(0,Math.sin((p[i]-a[i])/2),.00001);
    }
    @Test void seamDoesNotOverlap(){
        float[] a={3.13f,-3.13f,3.12f,-3.12f};var p=new ArrowLayout(24).place(a,4,100,30);
        for(int i=0;i<4;i++)for(int j=0;j<i;j++)assertTrue(200*Math.abs(Math.sin((p[i]-p[j])/2))>=29.99);
    }
}
