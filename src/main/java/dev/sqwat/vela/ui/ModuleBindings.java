package dev.sqwat.vela.ui;
import dev.sqwat.vela.core.VelaServices;
import dev.sqwat.vela.module.ClientModule;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import java.util.HashSet;
import java.util.Set;
public final class ModuleBindings {
    private static final Set<String> down = new HashSet<>();
    private static final Set<String> held = new HashSet<>();
    private ModuleBindings() {}
    public static void tick(Minecraft client, VelaServices services) {
        boolean allowed = client.level != null && client.gui.screen() == null && client.isWindowActive();
        for (ClientModule module : services.modules().all()) {
            boolean pressed = BindInput.down(client.getWindow().handle(),module.boundKey());
            boolean wasDown = down.contains(module.id());
            if (pressed) down.add(module.id()); else down.remove(module.id());
            if (module.bindMode() == ClientModule.BindMode.HOLD) {
                if (allowed && pressed && !wasDown) { held.add(module.id()); module.setEnabled(true); UiSounds.play(services, true); }
                if ((!allowed || !pressed) && held.remove(module.id())) module.setEnabled(false);
            } else if (allowed && pressed && !wasDown) { module.toggle(); UiSounds.play(services, module.enabled()); services.configs().requestSave(); }
        }
    }
}
