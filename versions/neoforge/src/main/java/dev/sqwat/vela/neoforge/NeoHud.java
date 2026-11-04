package dev.sqwat.vela.neoforge;

import dev.sqwat.vela.VelaClient;
import dev.sqwat.vela.ui.render.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Set;

/**
 * The hotbar group's Drag Mode offset on NeoForge. Fabric moves Hud.extractHotbarAndDecorations as a whole
 * (the hud position mixin); NeoForge draws the hotbar, the bars and the item name as separate GUI layers, so each of
 * them gets the same transform.
 */
final class NeoHud {
    private static final Set<Identifier> HOTBAR_GROUP = Set.of(VanillaGuiLayers.HOTBAR, VanillaGuiLayers.PLAYER_HEALTH,
        VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL, VanillaGuiLayers.VEHICLE_HEALTH, VanillaGuiLayers.AIR_LEVEL,
        VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
        VanillaGuiLayers.SELECTED_ITEM_NAME, VanillaGuiLayers.SPECTATOR_TOOLTIP);
    private static boolean pushed;

    private NeoHud() {}

    static void register() {
        // last, and only for layers nobody cancelled: a cancelled layer gets no Post to pop in
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, RenderGuiLayerEvent.Pre.class, e -> {
            if (HOTBAR_GROUP.contains(e.getName())) pushed = push(e.getGuiGraphics());
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, RenderGuiLayerEvent.Post.class, e -> {
            if (pushed && HOTBAR_GROUP.contains(e.getName())) e.getGuiGraphics().pose().popMatrix();
            pushed = false;
        });
    }

    /** The hud position mixin's hotbar transform. */
    private static boolean push(GuiGraphicsExtractor g) {
        if (VelaClient.services() == null) return false;
        var client = Minecraft.getInstance();
        var layout = VelaClient.services().hudLayout();
        layout.viewport(client.getWindow().getGuiScale());
        var player = client.player;
        int halfWidth = player != null && !player.getOffhandItem().isEmpty() ? 120 : 92;
        int rows = player == null ? 1 : Math.max(1, (int) Math.ceil((player.getMaxHealth() + player.getAbsorptionAmount()) / 20f));
        int height = player != null && player.getAbilities().instabuild ? 24 : 50 + (rows - 1) * Math.max(3, 12 - rows);
        int x = g.guiWidth() / 2 - halfWidth, y = g.guiHeight() - height;
        layout.recordNative("hotbar", new Rect(x, y, halfWidth * 2, height));
        var offset = layout.translation("hotbar", g.guiWidth(), g.guiHeight());
        g.pose().pushMatrix();
        g.pose().translate(offset.x() + x, offset.y() + y);
        g.pose().scale(layout.scale("hotbar"));
        g.pose().translate(-x, -y);
        return true;
    }
}
