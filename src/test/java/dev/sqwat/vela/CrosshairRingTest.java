package dev.sqwat.vela;
import dev.sqwat.vela.ui.render.CrosshairRing;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrosshairRingTest {
    private record Vertex(float x,float y,int color){}
    @Test void cachedGeometryExactlyMatchesOriginal(){
        for(float sweep:new float[]{0,.001f,.1f,.49f,.99f,1})
        for(float density:new float[]{1,2,4}){
            var actual=new java.util.ArrayList<Vertex>();
            var expected=new java.util.ArrayList<Vertex>();
            CrosshairRing.emit((x,y,c)->actual.add(new Vertex(x,y,c)),12,17,5,1.6f,sweep,0xFF756CFF,density);
            original(expected,1,0xC0080A10,density,false);
            if(sweep>0)original(expected,sweep,0xFF756CFF,density,true);
            assertEquals(expected,actual,"sweep="+sweep+", density="+density);
        }
    }
    private static void original(java.util.List<Vertex> out,float sweep,int color,float density,boolean gradient){
        float inner=5-1.6f/2,outer=5+1.6f/2,feather=Math.min(inner,Math.min(1.6f/2,1/density));
        int segments=Math.max(1,(int)Math.ceil(64*sweep));
        for(int i=0;i<segments;i++){
            float t0=(float)i/segments,t1=(float)(i+1)/segments;
            double a0=t0*sweep*Math.PI*2,a1=t1*sweep*Math.PI*2;
            float c0=(float)Math.cos(a0),s0=(float)Math.sin(a0),c1=(float)Math.cos(a1),s1=(float)Math.sin(a1);
            int col0=gradient?dev.sqwat.vela.ui.render.VelaTheme.mix(color,0xFFFFFFFF,.3f*(1-Math.abs(1-2*t0))):color;
            int col1=gradient?dev.sqwat.vela.ui.render.VelaTheme.mix(color,0xFFFFFFFF,.3f*(1-Math.abs(1-2*t1))):color;
            float[] radii={inner-feather,inner,outer,outer+feather};
            for(int band=0;band<3;band++){
                int in0=band==0?col0&0xFFFFFF:col0,in1=band==0?col1&0xFFFFFF:col1;
                int out0=band==2?col0&0xFFFFFF:col0,out1=band==2?col1&0xFFFFFF:col1;
                out.add(new Vertex(12+c0*radii[band],17+s0*radii[band],in0));
                out.add(new Vertex(12+c1*radii[band],17+s1*radii[band],in1));
                out.add(new Vertex(12+c1*radii[band+1],17+s1*radii[band+1],out1));
                out.add(new Vertex(12+c0*radii[band+1],17+s0*radii[band+1],out0));
            }
        }
    }
    @Test void arcHasBoundedFiniteGeometryAtAllConfiguredSizes(){
        for(float radius:new float[]{3,5,11})for(float width:new float[]{1.6f,4.6f})for(float scale:new float[]{1,2,4})for(float sweep:new float[]{0,.01f,.5f,1}){
            int[] count={0};float max=radius+width/2+1;
            CrosshairRing.emit((x,y,c)->{assertTrue(Float.isFinite(x)&&Float.isFinite(y));assertTrue(Math.hypot(x,y)<=max+.001);count[0]++;},0,0,radius,width,sweep,0xFF756CFF,scale);
            assertEquals(0,count[0]%4);assertTrue(count[0]<=64*3*4*2);
        }
    }
    @Test void emptySwingArcLeavesOnlyDarkBackground(){
        int[] colored={0};
        CrosshairRing.emit((x,y,c)->{if((c&0xFFFFFF)!=0x080A10)colored[0]++;},0,0,5,1.6f,0,0xFF756CFF,2);
        assertEquals(0,colored[0]);
    }
}
