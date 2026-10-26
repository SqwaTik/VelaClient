package dev.sqwat.vela.render.effect;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.List;

/** Tick-owned bounded path, published as an immutable render snapshot. */
public final class TrailHistory {
    public static final int LIMIT=64;
    public record Point(Vec3 position,long born){}
    private final ArrayDeque<Point> points=new ArrayDeque<>();
    public void clear(){points.clear();}
    public void expire(long now,long lifetime){
        while(!points.isEmpty()&&now-points.getFirst().born()>=lifetime)points.removeFirst();
    }
    public void update(Vec3 position,long now,long lifetime){
        expire(now,lifetime);
        if(!points.isEmpty()){
            double distance=points.getLast().position().distanceToSqr(position);
            if(distance>16)points.clear();
            else if(distance<.0025)return;
        }
        points.addLast(new Point(position,now));
        while(points.size()>LIMIT)points.removeFirst();
    }
    public List<Point> snapshot(){return List.copyOf(points);}
}
