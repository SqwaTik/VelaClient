package dev.sqwat.vela.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.LinkedHashMap;
import java.util.Map;

/** Conservative broad phase; negative results live for exactly one camera update. */
public final class SectionVisibilityCache {
    private static final class Entry {
        final AABB bounds;long frame=-1;boolean outside;
        Entry(int x,int y,int z){bounds=new AABB(x-8,y-8,z-8,x+24,y+24,z+24);}
    }
    private final Map<Long,Entry> sections=new LinkedHashMap<>(256,.75f,true){
        protected boolean removeEldestEntry(Map.Entry<Long,SectionVisibilityCache.Entry> e){return size()>4096;}
    };
    private Frustum frustum;private Vec3 camera;private long frame,tests;
    public void beginFrame(Frustum frustum,Vec3 camera){this.frustum=frustum;this.camera=camera;frame++;}
    public boolean outside(int x,int y,int z){
        if(frustum==null)return false;
        int sx=x>>4,sy=y>>4,sz=z>>4;long key=SectionPos.asLong(sx,sy,sz);
        Entry e=sections.get(key);if(e==null){e=new Entry(sx<<4,sy<<4,sz<<4);sections.put(key,e);}
        if(e.frame!=frame){e.frame=frame;tests++;e.outside=!e.bounds.contains(camera)&&!frustum.isVisible(e.bounds);}
        return e.outside;
    }
    public long tests(){return tests;}
    public void clear(){sections.clear();frustum=null;camera=null;}
}
