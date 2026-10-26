package dev.sqwat.vela.render;

/** Tick-based visual motion; the server's item position and collision stay authoritative. */
public final class DroppedItemMotion {
    private float previous,angle;
    private int lastTick=Integer.MIN_VALUE;
    public float sample(int tick,boolean grounded,float partial){
        return sample(tick,grounded,false,false,partial);
    }
    public float sample(int tick,boolean grounded,boolean water,boolean threeDimensional,float partial){
        if(lastTick==Integer.MIN_VALUE){angle=grounded&&!threeDimensional?90:0;previous=angle;lastTick=tick;}
        if(lastTick!=tick){
            previous=angle;
            int elapsed=Math.clamp(tick-lastTick,1,20);
            float target=90+180*Math.round((angle-90)/180);
            angle=grounded?(threeDimensional?angle:angle+(target-angle)*(float)(1-Math.pow(.62,elapsed))):angle+(water?3:12)*elapsed;
            lastTick=tick;
        }
        return previous+(angle-previous)*Math.clamp(partial,0,1);
    }
    public static double floorOffset(double minY,double maxY,double minZ,double maxZ,float degrees){
        double radians=Math.toRadians(degrees),c=Math.cos(radians),s=Math.sin(radians);
        return -(Math.min(minY*c,maxY*c)+Math.min(-minZ*s,-maxZ*s));
    }
}
