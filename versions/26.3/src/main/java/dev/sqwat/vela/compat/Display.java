package dev.sqwat.vela.compat;

import net.minecraft.client.Minecraft;

/** 26.3 dropped {@code Window.getRefreshRate()}; the rate of the monitor the window is on, 0 when unknown. */
public final class Display {
    private Display() {}
    public static int refreshRate() {
        var monitor = Minecraft.getInstance().getWindow().findBestMonitor();
        return monitor == null ? 0 : Math.round(monitor.currentMode().getRefreshRate());
    }
}
