package dev.sqwat.vela.render;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Native intersection semantics, with an allocation-free broad phase per segment. */
public final class ProjectileCollisions {
    private ProjectileCollisions() {}
    public static Vec3 nearest(Vec3 from,Vec3 end,List<AABB> boxes){
        Vec3 nearest=end;
        for(var box:boxes){
            if(Math.max(from.x,nearest.x)<box.minX||Math.min(from.x,nearest.x)>box.maxX
                ||Math.max(from.y,nearest.y)<box.minY||Math.min(from.y,nearest.y)>box.maxY
                ||Math.max(from.z,nearest.z)<box.minZ||Math.min(from.z,nearest.z)>box.maxZ)continue;
            var crossing=box.clip(from,nearest);
            if(crossing.isPresent())nearest=crossing.get();
        }
        return nearest;
    }
}
