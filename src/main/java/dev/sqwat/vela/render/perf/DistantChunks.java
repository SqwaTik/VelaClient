package dev.sqwat.vela.render.perf;

import dev.sqwat.vela.module.Optimization;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusFlowerBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LilyPadBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.LongConsumer;

/**
 * Simpler meshes for sections far from the camera: leaves drawn opaque with the faces between them culled (what vanilla
 * does with cutout leaves off), and grass, flowers and similar small plants left out. Crops, lily pads and anything
 * that glows stay. A section is compiled at the level its distance asks for; when the camera moves far enough for that
 * to change, it is marked dirty and vanilla recompiles it.
 */
public final class DistantChunks {
    /** Horizontal distance in sections (128 blocks) from which a section is simplified. */
    public static final int DISTANCE = 8;
    // Bands around DISTANCE before a compiled section is redone, so moving back and forth does not recompile it each time.
    // Going simple is only a saving, so it waits for a wider band and batches.
    private static final int DETAIL_BAND = 1, SIMPLIFY_BAND = 3;

    private static final ThreadLocal<boolean[]> compilingSimple = ThreadLocal.withInitial(() -> new boolean[1]);
    // Section node -> level it was compiled at (1 simple, 0 full); written by compile threads, read on the render thread.
    private static final Long2ByteMap compiled = Long2ByteMaps.synchronize(new Long2ByteOpenHashMap());
    private static volatile int cameraX, cameraZ;
    private static volatile boolean enabled = true;
    private static int lastCheckX = Integer.MIN_VALUE, lastCheckZ, framesSinceCheck;
    private static boolean lastEnabled = true;

    private DistantChunks() {}

    /** Called on the render thread before section updates are collected; marks sections compiled at the wrong level. */
    public static void beforeSectionUpdates(int cameraSectionX, int cameraSectionZ, List<SectionRenderDispatcher.RenderSection> visible, LongConsumer markDirty) {
        boolean on = Optimization.distantLodOn();
        enabled = on;
        cameraX = cameraSectionX;
        cameraZ = cameraSectionZ;
        // Every move to another section, every toggle, and every half second for sections that just came into view.
        boolean moved = cameraSectionX != lastCheckX || cameraSectionZ != lastCheckZ;
        if (!moved && on == lastEnabled && ++framesSinceCheck < 30) return;
        lastCheckX = cameraSectionX;
        lastCheckZ = cameraSectionZ;
        lastEnabled = on;
        framesSinceCheck = 0;
        synchronized (compiled) {
            if (compiled.isEmpty()) return;
            for (int i = 0, n = visible.size(); i < n; i++) {
                long node = visible.get(i).getSectionNode();
                if (!compiled.containsKey(node)) continue;
                byte level = compiled.get(node);
                int distance = distance(node, cameraSectionX, cameraSectionZ);
                boolean redo = level == 1 ? !on || distance < DISTANCE - DETAIL_BAND : on && distance > DISTANCE + SIMPLIFY_BAND;
                if (redo) {
                    compiled.remove(node);
                    markDirty.accept(node);
                }
            }
            // Sections left far behind are gone from the view; forget them so the map stays the size of the view.
            if (compiled.size() > 65536) compiled.keySet().removeIf((java.util.function.LongPredicate) node -> distance(node, cameraSectionX, cameraSectionZ) > 64);
        }
    }

    /** Wraps one section compile on a compile thread. */
    public static void beginCompile(SectionPos pos) {
        boolean simple = enabled && distance(pos.asLong(), cameraX, cameraZ) >= DISTANCE;
        compilingSimple.get()[0] = simple;
        compiled.put(pos.asLong(), simple ? (byte) 1 : (byte) 0);
    }

    public static void endCompile() {
        compilingSimple.get()[0] = false;
    }

    /** True while this thread compiles a far section. */
    public static boolean simplifying() {
        return compilingSimple.get()[0];
    }

    /** Small plants that are left out of far sections. */
    public static boolean skipped(BlockState state) {
        Block block = state.getBlock();
        if (!(block instanceof VegetationBlock) || state.getLightEmission() > 0) return false;
        return !(block instanceof CropBlock || block instanceof StemBlock || block instanceof AttachedStemBlock || block instanceof AzaleaBlock
            || block instanceof LilyPadBlock || block instanceof NetherWartBlock || block instanceof SweetBerryBushBlock
            || block instanceof CactusFlowerBlock || block instanceof MushroomBlock);
    }

    private static int distance(long node, int cameraSectionX, int cameraSectionZ) {
        return Math.max(Math.abs(SectionPos.x(node) - cameraSectionX), Math.abs(SectionPos.z(node) - cameraSectionZ));
    }
}
