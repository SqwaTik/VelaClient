package dev.sqwat.vela.neoforge.shim.fabric.api.client.screen.v1;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

public final class Screens {
    private Screens() {}

    /** The screen's widgets (a copy: Fabric's list writes through, Vela only reads it). */
    public static List<AbstractWidget> getWidgets(Screen screen) {
        var widgets = new ArrayList<AbstractWidget>();
        for (var child : screen.children()) if (child instanceof AbstractWidget widget) widgets.add(widget);
        return widgets;
    }
}
