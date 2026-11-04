package dev.sqwat.vela.render.perf;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.sqwat.vela.module.Optimization;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Map textures copied into shared pages, so a wall of framed maps is one draw per page instead of one per map (each map is
 * its own texture in vanilla, so its own draw, texture lookup and descriptor set). The vanilla texture stays as it is for
 * anything else that reads it; maps past the last page just keep drawing the vanilla way. Render thread only.
 */
public final class MapAtlas {
    private static final int MAP = 128;
    private static final int SIZE = 2048;
    private static final int PER_ROW = SIZE / MAP;
    private static final int PER_PAGE = PER_ROW * PER_ROW;
    // 4 pages of 16 MB: 1024 maps, more than a large map-art wall.
    private static final int MAX_PAGES = 4;

    public record Slot(RenderType renderType, float u0, float v0, float u1, float v1, int page, int index) {}

    private static final class Page extends AbstractTexture {
        final Identifier location;
        final RenderType renderType;

        Page(int number) {
            GpuDevice device = RenderSystem.getDevice();
            texture = device.createTexture("Vela map atlas " + number, 5, GpuFormat.RGBA8_UNORM, SIZE, SIZE, 1, 1);
            sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
            textureView = device.createTextureView(texture);
            location = Identifier.fromNamespaceAndPath("velaclient", "map_atlas/" + number);
            Minecraft.getInstance().getTextureManager().register(location, this);
            renderType = RenderTypes.text(location);
        }
    }

    private static final List<Page> pages = new ArrayList<>();
    private static final ArrayDeque<Integer> free = new ArrayDeque<>();
    private static final Map<Identifier, Slot> slots = new HashMap<>();
    private static int used;

    private MapAtlas() {}

    /** Where this map texture is in the atlas, or null to draw it the vanilla way. */
    public static Slot slot(Identifier texture) {
        return texture == null || slots.isEmpty() || !Optimization.mapAtlasOn() ? null : slots.get(texture);
    }

    /** Copies the map's pixels (just uploaded to its own texture) into its slot, taking a slot first if it has none. */
    public static void upload(Identifier texture, NativeImage pixels) {
        if (!Optimization.mapAtlasOn() || pixels.getWidth() != MAP || pixels.getHeight() != MAP) return;
        Slot slot = slots.get(texture);
        if (slot == null && (slot = allocate(texture)) == null) return;
        int x = slot.index % PER_ROW * MAP, y = slot.index / PER_ROW * MAP;
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(pages.get(slot.page).getTexture(), pixels, 0, 0, x, y);
    }

    /** For a map that skipped its upload: copies it in only if it is not in the atlas yet. */
    public static void ensure(Identifier texture, NativeImage pixels) {
        if (!slots.containsKey(texture)) upload(texture, pixels);
    }

    /** The map's texture is gone (level change): its slot is free again. */
    public static void release(Identifier texture) {
        Slot slot = slots.remove(texture);
        if (slot != null) free.push(slot.page * PER_PAGE + slot.index);
    }

    private static Slot allocate(Identifier texture) {
        int id;
        if (!free.isEmpty()) id = free.pop();
        else if (used < MAX_PAGES * PER_PAGE) id = used++;
        else return null;
        int page = id / PER_PAGE, index = id % PER_PAGE;
        while (pages.size() <= page) pages.add(new Page(pages.size()));
        float u0 = (float) (index % PER_ROW * MAP) / SIZE, v0 = (float) (index / PER_ROW * MAP) / SIZE;
        float step = (float) MAP / SIZE;
        Slot slot = new Slot(pages.get(page).renderType, u0, v0, u0 + step, v0 + step, page, index);
        slots.put(texture, slot);
        return slot;
    }
}
