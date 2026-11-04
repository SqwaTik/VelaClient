package dev.sqwat.vela.compat;

import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;

/** Fabric's 1.21.6 HUD element registry over 1.21.5's layered drawer: first goes under the vanilla layers, last over them. */
public final class HudElementRegistry {
    private HudElementRegistry(){}
    public static void addLast(ResourceLocation id,LayeredDraw.Layer element){
        HudLayerRegistrationCallback.EVENT.register(drawer->drawer.addLayer(IdentifiedLayer.of(id,element)));
    }
    public static void addFirst(ResourceLocation id,LayeredDraw.Layer element){
        HudLayerRegistrationCallback.EVENT.register(drawer->drawer.attachLayerBefore(IdentifiedLayer.MISC_OVERLAYS,IdentifiedLayer.of(id,element)));
    }
}
