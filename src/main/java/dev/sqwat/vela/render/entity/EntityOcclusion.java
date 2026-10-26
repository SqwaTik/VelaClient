package dev.sqwat.vela.render.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Hides entities behind full opaque blocks after vanilla frustum and section culling accepted them.
 * Render thread only. Line-of-sight work has a per-frame time budget; results are cached briefly per entity,
 * and an entity without a fresh result keeps its last answer (new entities start visible, so nothing pops out).
 */
public final class EntityOcclusion {
    public enum Decision {VISIBLE,HIDE_MODEL,HIDE}
    private static final long BUDGET_NANOS=250_000;
    private static final long VISIBLE_TTL=150_000_000L,HIDDEN_TTL=50_000_000L;
    private static final double NEAR_SQ=4*4,MOVE_SQ=.75*.75;
    private static final class Entry {long time;boolean visible;double x,y,z;}
    private static final Int2ObjectOpenHashMap<Entry> CACHE=new Int2ObjectOpenHashMap<>();
    private static final IntOpenHashSet HIDDEN_MODELS=new IntOpenHashSet();
    private static Object world;
    private static long frameStart,lastPrune;
    private static double eyeX,eyeY,eyeZ;
    private static boolean active;
    private static final ChunkBlocks BLOCKS=new ChunkBlocks();
    private EntityOcclusion(){}

    public static void beginFrame(net.minecraft.client.Camera camera){
        HIDDEN_MODELS.clear();
        var mc=Minecraft.getInstance();var level=mc.level;
        if(level!=world){CACHE.clear();world=level;BLOCKS.level=level;BLOCKS.cached=false;}
        var options=options();
        active=level!=null&&options!=null&&options.anyOcclusion()&&!camera.isPanoramicMode()&&camera.getCapturedFrustum()==null
            &&!(mc.player!=null&&mc.player.isSpectator());
        if(!active)return;
        var eye=camera.position();eyeX=eye.x;eyeY=eye.y;eyeZ=eye.z;
        BLOCKS.cached=false; // chunks may have loaded or unloaded since the last frame
        // Inside a block (third person against a wall) nothing can be judged reliably.
        if(BLOCKS.opaque(floor(eyeX),floor(eyeY),floor(eyeZ))){active=false;return;}
        frameStart=System.nanoTime();
        if(frameStart-lastPrune>2_000_000_000L){lastPrune=frameStart;CACHE.int2ObjectEntrySet().removeIf(e->frameStart-e.getValue().time>2_000_000_000L);}
    }
    public static void endEntities(){HIDDEN_MODELS.clear();}
    public static void clear(){CACHE.clear();HIDDEN_MODELS.clear();world=null;BLOCKS.level=null;BLOCKS.cached=false;BLOCKS.chunk=null;active=false;}
    public static boolean modelHidden(Entity entity){return !HIDDEN_MODELS.isEmpty()&&HIDDEN_MODELS.contains(entity.getId());}

    /** Called once per frustum-visible entity during level extraction. */
    public static Decision decide(Entity entity){
        if(!active)return Decision.VISIBLE;
        var options=options();if(options==null)return Decision.VISIBLE;
        Decision hidden=category(entity,options);
        if(hidden==Decision.VISIBLE)return Decision.VISIBLE;
        var mc=Minecraft.getInstance();
        if(entity==mc.player||entity==mc.getCameraEntity()||entity.isCurrentlyGlowing()||mc.shouldEntityAppearGlowing(entity))return Decision.VISIBLE;
        if(mc.player!=null&&(entity.hasPassenger(mc.player)||mc.player.hasPassenger(entity)))return Decision.VISIBLE;
        if(entity.distanceToSqr(eyeX,eyeY,eyeZ)<NEAR_SQ)return Decision.VISIBLE;
        if(visible(entity))return Decision.VISIBLE;
        if(hidden==Decision.HIDE_MODEL)HIDDEN_MODELS.add(entity.getId());
        return hidden;
    }
    private static Decision category(Entity entity,dev.sqwat.vela.module.ZakoOptimization options){
        // Players keep their (vanilla see-through) name tag; only model, armour, items, cape and shadow are skipped.
        if(entity instanceof AbstractClientPlayer)return options.occludePlayers()?Decision.HIDE_MODEL:Decision.VISIBLE;
        if(entity instanceof HangingEntity)return options.occludeHanging()?Decision.HIDE:Decision.VISIBLE;
        if(entity instanceof Display.TextDisplay text)
            return options.occludeHolograms()&&(((dev.sqwat.vela.mixin.entity.TextDisplayFlagsAccessor)text).vela$flags()&Display.TextDisplay.FLAG_SEE_THROUGH)==0?Decision.HIDE:Decision.VISIBLE;
        if(entity instanceof Display)return options.occludeHolograms()?Decision.HIDE:Decision.VISIBLE;
        if(entity instanceof ArmorStand)return options.occludeHolograms()?Decision.HIDE:Decision.VISIBLE;
        // A custom-named mob shows its tag through walls in vanilla: keep it.
        if(entity instanceof LivingEntity)return options.occludeMobs()&&!entity.hasCustomName()?Decision.HIDE:Decision.VISIBLE;
        return Decision.VISIBLE;
    }
    private static boolean visible(Entity entity){
        long now=System.nanoTime();
        Entry entry=CACHE.get(entity.getId());
        if(entry!=null){
            boolean fresh=now-entry.time<(entry.visible?VISIBLE_TTL:HIDDEN_TTL);
            double mx=entry.x-eyeX,my=entry.y-eyeY,mz=entry.z-eyeZ;
            if(fresh&&mx*mx+my*my+mz*mz<MOVE_SQ)return entry.visible;
        }
        if(now-frameStart>BUDGET_NANOS)return entry==null||entry.visible;
        var box=entity.getBoundingBox();
        boolean result=OcclusionRay.visible(BLOCKS,eyeX,eyeY,eyeZ,box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ);
        if(entry==null){entry=new Entry();CACHE.put(entity.getId(),entry);}
        entry.time=now;entry.visible=result;entry.x=eyeX;entry.y=eyeY;entry.z=eyeZ;
        return result;
    }
    private static dev.sqwat.vela.module.ZakoOptimization options(){
        var services=dev.sqwat.vela.VelaClient.services();return services==null?null:services.zakoOptimization();
    }
    private static int floor(double value){int i=(int)value;return value<i?i-1:i;}

    /** Chunk-cached block lookup; unloaded chunks and out-of-height positions never occlude. */
    private static final class ChunkBlocks implements OcclusionRay.Blocks {
        ClientLevel level;LevelChunk chunk;int chunkX,chunkZ;boolean cached;
        @Override public boolean opaque(int x,int y,int z){
            if(level==null||level.isOutsideBuildHeight(y))return false;
            int cx=x>>4,cz=z>>4;
            if(cx!=chunkX||cz!=chunkZ||!cached){chunk=level.getChunkSource().getChunk(cx,cz,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,false);chunkX=cx;chunkZ=cz;cached=true;}
            if(chunk==null)return false;
            var section=chunk.getSection(level.getSectionIndex(y));
            return !section.hasOnlyAir()&&section.getBlockState(x&15,y&15,z&15).isSolidRender();
        }
    }
}
