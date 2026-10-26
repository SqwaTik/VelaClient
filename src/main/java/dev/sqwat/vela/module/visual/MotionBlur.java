package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.NumberSetting;

public final class MotionBlur extends ClientModule {
    private final dev.sqwat.vela.module.setting.BooleanSetting thirdPerson=setting(new dev.sqwat.vela.module.setting.BooleanSetting("third_person","От третьего лица","Показывать размытие при виде от третьего лица",true));
    private final NumberSetting strength=setting(new NumberSetting("strength","Сила","Интенсивность следа при поворотах камеры",25,0,70,5));
    public MotionBlur(){super("motion_blur","Motion Blur","Размытие изображения при поворотах камеры",ModuleCategory.UTILITIES);}
    public static float strength(){
        var s=dev.sqwat.vela.VelaClient.services();
        return s!=null&&s.modules().byId("motion_blur") instanceof MotionBlur m&&m.enabled()
            &&(m.thirdPerson.value()||net.minecraft.client.Minecraft.getInstance().options.getCameraType().isFirstPerson())?m.strength.value().floatValue()/100:0;
    }
}
