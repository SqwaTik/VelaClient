package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ChoiceSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.upscale.Quality;
import dev.sqwat.vela.upscale.UpscalerRuntime;
import java.util.List;

/**
 * MineSR in the dashboard: the world is drawn at a lower resolution with a moving sub-pixel offset and rebuilt to full
 * resolution from several frames by a trained network; the hand and interface stay at full resolution. Pays off when
 * the GPU is the limit (improved transparency, high resolutions, weak GPUs); with a CPU-bound setup it changes little.
 */
public final class UpscalerModule extends ClientModule {
    private static final String QUALITY="Качество",BALANCED="Баланс",PERFORMANCE="Производительность",NATIVE="Только сглаживание";
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Режим","Во сколько раз меньше рисуется мир",QUALITY,List.of(QUALITY,BALANCED,PERFORMANCE,NATIVE)));
    private final NumberSetting sharpness=setting(new NumberSetting("sharpness","Резкость","Сила повышения резкости после апскейла, %",50,0,100,5));
    private final BooleanSetting neural=setting(new BooleanSetting("neural","Нейросеть","Решения апскейла принимает обученная сеть; выключите для ручных правил",true));
    private final BooleanSetting sharpTextures=setting(new BooleanSetting("sharp_textures","Чёткие текстуры","Текстуры детальнее при низком разрешении, чтобы апскейлер их восстановил",true));

    public UpscalerModule(){
        super("upscaler","Upscaler","Мир в меньшем разрешении, нейросеть достраивает до полного (MineSR)",ModuleCategory.VISUALS);
        require(UpscalerRuntime::available);
    }
    private Quality quality(){
        return switch(mode.value()){case BALANCED->Quality.BALANCED;case PERFORMANCE->Quality.PERFORMANCE;case NATIVE->Quality.NATIVE;default->Quality.QUALITY;};
    }
    private void push(boolean on){UpscalerRuntime.apply(on,quality(),sharpness.value().floatValue()/100f,neural.value(),sharpTextures.value());}
    @Override protected void onEnable(){push(true);}
    @Override protected void onDisable(){push(false);}
    @Override public void onClientTick(){push(true);}
}
