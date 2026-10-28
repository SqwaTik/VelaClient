package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.upscale.UpscalerRuntime;

/**
 * MineSR frame generation: between every two rendered frames an extra one is built from motion and depth, so the
 * picture moves more smoothly. Controls do not get faster (about half a frame of extra input delay). Works alone or
 * together with the Upscaler; with performance upscaling it gives the largest gain.
 */
public final class FrameGeneration extends ClientModule {
    public FrameGeneration(){
        super("frame_generation","Frame Generation","Дополнительный кадр между каждыми двумя: плавнее движение (MineSR)",ModuleCategory.VISUALS);
        require(UpscalerRuntime::available);
    }
    @Override protected void onEnable(){UpscalerRuntime.applyFrameGen(true);}
    @Override protected void onDisable(){UpscalerRuntime.applyFrameGen(false);}
    @Override public void onClientTick(){UpscalerRuntime.applyFrameGen(true);}
}
