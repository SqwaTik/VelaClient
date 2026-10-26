package dev.sqwat.vela;

import dev.sqwat.vela.render.OcclusionRay;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class OcclusionRayTest {
    private static OcclusionRay.Blocks of(Set<Long> solid){return (x,y,z)->solid.contains(key(x,y,z));}
    private static long key(int x,int y,int z){return ((long)x&0x1FFFFF)|(((long)y&0x1FFFFF)<<21)|(((long)z&0x1FFFFF)<<42);}

    @Test void openAirIsVisible(){
        assertTrue(OcclusionRay.visible((x,y,z)->false,.5,1.6,.5, 9.7,1,0.2, 10.3,2.8,0.8));
    }
    @Test void fullWallHidesPlayerBehindIt(){
        var wall=new HashSet<Long>();
        for(int y=-4;y<10;y++)for(int z=-6;z<7;z++)wall.add(key(5,y,z));
        assertFalse(OcclusionRay.visible(of(wall),.5,1.6,.5, 9.7,1,0.2, 10.3,2.8,0.8));
    }
    @Test void partlyCoveredBoxStaysVisible(){
        var wall=new HashSet<Long>();
        for(int y=-4;y<2;y++)for(int z=-6;z<7;z++)wall.add(key(5,y,z)); // waist-high wall, head sticks out
        assertTrue(OcclusionRay.visible(of(wall),.5,1.6,.5, 9.7,1,0.2, 10.3,2.8,0.8));
    }
    @Test void paintingOnItsOwnWallIsVisibleFromTheFront(){
        var wall=new HashSet<Long>();
        for(int y=0;y<4;y++)for(int z=-2;z<3;z++)wall.add(key(10,y,z));
        // 1/16 thick painting box touching the wall face at x=10
        assertTrue(OcclusionRay.visible(of(wall),2.5,1.6,.5, 9.9375,1,-1, 10,3,2));
        // the same painting seen from behind its wall
        var behind=new HashSet<>(wall);for(int y=0;y<4;y++)for(int z=-2;z<3;z++)behind.add(key(11,y,z));
        assertFalse(OcclusionRay.visible(of(behind),14.5,1.6,.5, 9.9375,1,-1, 10,3,2));
    }
    @Test void negativeCoordinatesUseFloor(){
        var wall=new HashSet<Long>();
        for(int y=-4;y<10;y++)for(int z=-12;z<0;z++)wall.add(key(-5,y,z));
        assertFalse(OcclusionRay.visible(of(wall),-.5,1.6,-5.5, -9.7,1,-6.2, -9.1,2.8,-5.6));
    }
    /** Exact overlap length of the segment with a unit voxel (slab method), or a negative value if they miss. */
    private static double overlap(double ax,double ay,double az,double bx,double by,double bz,long voxel){
        int[] v={(int)(voxel<<43>>43),(int)(voxel<<22>>43),(int)(voxel<<1>>43)};
        double[] a={ax,ay,az},d={bx-ax,by-ay,bz-az};double t0=0,t1=1;
        for(int i=0;i<3;i++){
            if(d[i]==0){if(a[i]<v[i]||a[i]>v[i]+1)return -1;continue;}
            double u=(v[i]-a[i])/d[i],w=(v[i]+1-a[i])/d[i];if(u>w){double q=u;u=w;w=q;}
            t0=Math.max(t0,u);t1=Math.min(t1,w);
        }
        return t1-t0;
    }
    /** The traversal agrees with an exact segment/voxel test, apart from lines that only graze an edge. */
    @Test void matchesExactSegmentTest(){
        var random=new Random(26);int checked=0;
        for(int trial=0;trial<5000;trial++){
            var solid=new HashSet<Long>();
            for(int i=0;i<40;i++)solid.add(key(random.nextInt(16)-8,random.nextInt(16)-8,random.nextInt(16)-8));
            double ax=random.nextDouble()*16-8,ay=random.nextDouble()*16-8,az=random.nextDouble()*16-8;
            double bx=random.nextDouble()*16-8,by=random.nextDouble()*16-8,bz=random.nextDouble()*16-8;
            long start=key((int)Math.floor(ax),(int)Math.floor(ay),(int)Math.floor(az)),end=key((int)Math.floor(bx),(int)Math.floor(by),(int)Math.floor(bz));
            boolean clear=true,grazing=false;
            for(long voxel:solid){
                if(voxel==start||voxel==end)continue;
                double length=overlap(ax,ay,az,bx,by,bz,voxel);
                if(length>1e-9)clear=false;else if(length>=0)grazing=true;
            }
            if(grazing)continue;
            // Point boxes reduce visible() to the centre ray.
            assertEquals(clear,OcclusionRay.visible(of(solid),ax,ay,az,bx,by,bz,bx,by,bz),"trial "+trial);
            checked++;
        }
        assertTrue(checked>4000);
    }
}
