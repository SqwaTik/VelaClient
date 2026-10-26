package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.ChoiceSetting;
import java.util.List;

public final class AspectRatio extends ClientModule {
    private final ChoiceSetting ratio = setting(new ChoiceSetting("ratio", "Соотношение", "Пропорции игровой камеры", "16:9", List.of("16:9", "4:3", "1:1", "21:9")));
    public AspectRatio() { super("aspect_ratio", "Aspect Ratio", "Соотношение сторон игрового мира", ModuleCategory.UTILITIES); }
    public static float value(String value) {
        return switch (value) { case "4:3" -> 4f/3f; case "1:1" -> 1; case "21:9" -> 21f/9f; default -> 16f/9f; };
    }
    public static float apply(float original) {
        var services = dev.sqwat.vela.VelaClient.services();
        if (services != null && services.modules().byId("aspect_ratio") instanceof AspectRatio module && module.enabled()) return value(module.ratio.value());
        return original;
    }
}
