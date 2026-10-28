package dev.sqwat.vela.render.effect;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A blast front that travels along the ground. Computed once per explosion: a grid of half-block cells around the
 * centre, each with the walkable floor height and the shortest path length from the centre around walls (8-way,
 * one-block steps up or down). The front at radius R is the R contour of that field, so it bends around obstacles,
 * fills corridors and leaves a weaker "shadow" behind walls (path much longer than the straight line).
 */
public final class ShockFront {
    public static final double CELL=.5;
    private static final int STEP_DOWN=3;
    /** Solid-for-the-blast test on block coordinates. */
    public interface Terrain{boolean blocked(int x,int y,int z);}
    public record Segment(Vec3 a,Vec3 b,double strengthA,double strengthB){}
    private final Vec3 center;
    private final int half,size;
    private final float[] distance,floor;

    private ShockFront(Vec3 center,int half,float[] distance,float[] floor){
        this.center=center;this.half=half;this.size=half*2+1;this.distance=distance;this.floor=floor;
    }

    /** Open ground at the centre's height: plain circles (used without a world, e.g. in tests). */
    public static ShockFront open(Vec3 center,double radius){return compute(center,radius,(x,y,z)->y<Math.floor(center.y));}

    public static ShockFront compute(Vec3 center,double radius,Terrain terrain){
        int half=(int)Math.ceil((radius+1)/CELL),size=half*2+1,cells=size*size;
        float[] floor=new float[cells],distance=new float[cells];
        int baseY=(int)Math.floor(center.y+.01);
        for(int j=0;j<size;j++)for(int i=0;i<size;i++){
            int bx=(int)Math.floor(center.x+(i-half)*CELL),bz=(int)Math.floor(center.z+(j-half)*CELL);
            floor[j*size+i]=floorAt(terrain,bx,baseY,bz);
            distance[j*size+i]=Float.POSITIVE_INFINITY;
        }
        int start=half*size+half;
        if(Float.isNaN(floor[start]))floor[start]=baseY;
        distance[start]=0;
        var queue=new PriorityQueue<long[]>((a,b)->Double.compare(Double.longBitsToDouble(a[0]),Double.longBitsToDouble(b[0])));
        queue.add(new long[]{Double.doubleToLongBits(0),start});
        float limit=(float)(radius+1.5);
        while(!queue.isEmpty()){
            var item=queue.poll();double d=Double.longBitsToDouble(item[0]);int index=(int)item[1];
            if(d>distance[index]||d>limit)continue;
            int i=index%size,j=index/size;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
                if(dx==0&&dz==0)continue;
                int ni=i+dx,nj=j+dz;if(ni<0||nj<0||ni>=size||nj>=size)continue;
                int next=nj*size+ni;
                if(!passable(floor,index,next))continue;
                // No cutting wall corners on diagonals.
                if(dx!=0&&dz!=0&&(!passable(floor,index,j*size+ni)||!passable(floor,index,nj*size+i)))continue;
                double nd=d+CELL*(dx!=0&&dz!=0?1.41421356:1)+Math.abs(floor[next]-floor[index])*.5;
                if(nd<distance[next]){distance[next]=(float)nd;queue.add(new long[]{Double.doubleToLongBits(nd),next});}
            }
        }
        return new ShockFront(center,half,distance,floor);
    }

    private static boolean passable(float[] floor,int from,int to){
        return !Float.isNaN(floor[to])&&Math.abs(floor[to]-floor[from])<=1.01f;
    }

    /** Ground under the blast layer: the same level, a step up, or up to three blocks down. NaN means a wall. */
    private static float floorAt(Terrain terrain,int x,int y,int z){
        if(terrain.blocked(x,y,z)){
            return !terrain.blocked(x,y+1,z)&&!terrain.blocked(x,y+2,z)?y+1:Float.NaN;
        }
        if(terrain.blocked(x,y+1,z))return Float.NaN; // low ceiling / overhang wall at head height
        for(int d=0;d<=STEP_DOWN;d++)if(terrain.blocked(x,y-d-1,z))return y-d;
        return y-STEP_DOWN; // open air below: keep the front flat
    }

    public Vec3 center(){return center;}

    /** Path length from the centre to this point, or the straight distance outside the grid. */
    public double distanceAt(double x,double z){
        int i=(int)Math.round((x-center.x)/CELL)+half,j=(int)Math.round((z-center.z)/CELL)+half;
        if(i<0||j<0||i>=size||j>=size)return Math.hypot(x-center.x,z-center.z);
        return distance[j*size+i];
    }

    /** The front at the given radius as line segments on the ground, with 0..1 strength (1 = direct line of sight). */
    public List<Segment> contour(double radius){
        var out=new ArrayList<Segment>();
        for(int j=0;j<size-1;j++)for(int i=0;i<size-1;i++){
            int a=j*size+i,b=a+1,c=a+size+1,d=a+size;
            float da=distance[a],db=distance[b],dc=distance[c],dd=distance[d];
            if(!Float.isFinite(da)||!Float.isFinite(db)||!Float.isFinite(dc)||!Float.isFinite(dd))continue;
            int mask=(da<radius?1:0)|(db<radius?2:0)|(dc<radius?4:0)|(dd<radius?8:0);
            if(mask==0||mask==15)continue;
            Vec3 ab=edge(a,b,i,j,i+1,j,radius),bc=edge(b,c,i+1,j,i+1,j+1,radius),cd=edge(d,c,i,j+1,i+1,j+1,radius),da2=edge(a,d,i,j,i,j+1,radius);
            switch(mask){
                case 1,14->add(out,da2,ab);
                case 2,13->add(out,ab,bc);
                case 3,12->add(out,da2,bc);
                case 4,11->add(out,bc,cd);
                case 6,9->add(out,ab,cd);
                case 7,8->add(out,da2,cd);
                case 5->{add(out,da2,ab);add(out,bc,cd);}
                case 10->{add(out,ab,bc);add(out,cd,da2);}
                default->{}
            }
        }
        return out;
    }

    private Vec3 edge(int a,int b,int ia,int ja,int ib,int jb,double radius){
        float va=distance[a],vb=distance[b];
        double t=Math.abs(vb-va)<1e-6?.5:Math.clamp((radius-va)/(vb-va),0,1);
        double x=center.x+((ia+(ib-ia)*t)-half)*CELL,z=center.z+((ja+(jb-ja)*t)-half)*CELL;
        double y=floor[a]+(floor[b]-floor[a])*t;
        return new Vec3(x,y,z);
    }

    private void add(List<Segment> out,Vec3 a,Vec3 b){out.add(new Segment(a,b,strength(a),strength(b)));}

    /** Line-of-sight share: the straight distance over the path length, so the front behind walls is fainter. */
    private double strength(Vec3 p){
        double path=distanceAt(p.x,p.z),straight=Math.hypot(p.x-center.x,p.z-center.z);
        if(!(path>1e-3))return 1;
        double r=Math.clamp(straight/path,0,1);
        return r*r*r;
    }
}
