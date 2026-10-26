package dev.sqwat.vela.render;

import net.minecraft.client.gui.screens.Overlay;

/** Input is available while old resources are intact, not during the apply barrier. */
public interface InteractiveReload {
    boolean vela$allowsInput();
    boolean vela$showsWorld();
    static Overlay inputOverlay(Overlay overlay){
        return overlay instanceof InteractiveReload reload&&reload.vela$allowsInput()?null:overlay;
    }
}
