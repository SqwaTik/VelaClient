package dev.sqwat.vela.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.AABB;
import java.util.LinkedHashMap;
import java.util.Map;

/** Render-only fast path. Does not alter chunks, ticks, networking or depth state. */
public final class AutomaticVisibility {
    private static Object level;
    private static final SectionVisibilityCache SECTIONS=new SectionVisibilityCache();
    public static void beginFrame(){
        PerformanceCapture.frame();
        var c=Minecraft.getInstance();var camera=c.gameRenderer.mainCamera();
        if(level!=c.level){clear();level=c.level;}
        if(c.level==null||!camera.isInitialized()||camera.isPanoramicMode()||camera.getCapturedFrustum()!=null){SECTIONS.clear();return;}
        SECTIONS.beginFrame(camera.getCullFrustum(),camera.position());
    }
    private record PlayerBounds(AABB source,AABB expanded){}
    private static final Map<net.minecraft.world.entity.Entity,PlayerBounds> PLAYERS=new java.util.WeakHashMap<>();
    private static final Map<Long,AABB> BOUNDS=new LinkedHashMap<>(256,.75f,true){
        protected boolean removeEldestEntry(Map.Entry<Long,AABB> entry){return size()>4096;}
    };
    private AutomaticVisibility(){}
    public static boolean outsideView(BlockEntity entity,boolean global){
        if(global||!(entity instanceof ChestBlockEntity||entity instanceof ShulkerBoxBlockEntity||entity instanceof EnderChestBlockEntity||entity instanceof DecoratedPotBlockEntity||entity instanceof SkullBlockEntity))return false;
        var client=Minecraft.getInstance();var camera=client.gameRenderer.mainCamera();
        if(client.level==null||!camera.isInitialized()||camera.isPanoramicMode()||camera.getCapturedFrustum()!=null)return false;
        if(level!=client.level){level=client.level;BOUNDS.clear();}
        var pos=entity.getBlockPos();
        if(SECTIONS.outside(pos.getX(),pos.getY(),pos.getZ()))return true;
        long key=pos.asLong();AABB box=BOUNDS.get(key);
        if(box==null){var b=VisibilityBounds.storage(pos.getX(),pos.getY(),pos.getZ());box=new AABB(b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ());BOUNDS.put(key,box);}
        if(box.contains(camera.position()))return false;
        return !camera.getCullFrustum().isVisible(box);
    }
    public static AABB playerBounds(net.minecraft.world.entity.Entity entity){
        AABB source=entity.getBoundingBox();PlayerBounds cached=PLAYERS.get(entity);
        if(cached==null||!cached.source().equals(source)){cached=new PlayerBounds(source,source.inflate(4));PLAYERS.put(entity,cached);}
        return cached.expanded();
    }
    public static void clear(){level=null;BOUNDS.clear();PLAYERS.clear();SECTIONS.clear();NameTagTextCache.clear();SignTextWidths.clear();}
}
