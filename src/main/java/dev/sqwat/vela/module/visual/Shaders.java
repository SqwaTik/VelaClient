package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.render.VelaTheme;

/** Settings for installed shader passes. New passes are exposed only when implemented. */
public final class Shaders extends ClientModule {
    private final BooleanSetting sky=setting(new BooleanSetting("sky","Небо","Процедурные эффекты небосвода",false));
    private final ChoiceSetting skyMode=setting(new ChoiceSetting("sky_mode","Режим неба","Эффект небосвода","Северное сияние",java.util.List.of("Северное сияние","Звездопад","Чёрная дыра","GLSL")));
    private final Setting<String> skyCode=setting(new Setting<String>("sky_code","Код неба","",dev.sqwat.vela.render.shader.CustomSkySource.DEFAULT){
        @Override protected String validate(String value){return dev.sqwat.vela.render.shader.CustomSkySource.validate(value);}
    });
    public String skyCode(){return skyCode.value();}
    public void skyCode(String source){skyCode.set(source);}
    private String editorDraft;
    public String editorDraft(){return editorDraft==null?skyCode():editorDraft;}
    public void editorDraft(String source){editorDraft=source;}
    public void useCustomSky(){sky.set(true);skyMode.set("GLSL");setEnabled(true);}
    private final NumberSetting skyBrightness=setting(new NumberSetting("sky_brightness","Яркость неба","Интенсивность свечения",100,10,200,5));
    private final BooleanSetting skyTheme=setting(new BooleanSetting("sky_theme","Цвет клиента","Использовать цвет клиента для неба",true));
    private final ColorSetting skyColor=setting(new ColorSetting("sky_color","Цвет неба",0x756CFF));
    public boolean skyActive(){return sky.value();}
    public String skyMode(){return skyMode.value();}
    public float skyBrightness(){return skyBrightness.value().floatValue()/100;}
    public int skyColor(){return skyTheme.value()?VelaTheme.PRIMARY:skyColor.value();}
    private final BooleanSetting hands=setting(new BooleanSetting("hands","Руки","Шейдер рук и предметов",false));
    private final ChoiceSetting handMode=setting(new ChoiceSetting("hand_mode","Режим рук","Эффект рук и предметов","Голограмма",java.util.List.of("Голограмма","Стекло","Пламя")));
    public String handMode(){return handMode.value();}
    public boolean handsActive(){return hands.value();}
    public int handColor(){return VelaTheme.PRIMARY;}
    private final BooleanSetting wet=setting(new BooleanSetting("wet","Мокрые поверхности","Отражения на верхних гранях блоков",false));
    private final NumberSetting reflection=setting(new NumberSetting("wet_reflection","Отражение","Яркость отражения мира",70,0,100,5));
    private final NumberSetting wetness=setting(new NumberSetting("wet_amount","Влажность","Насыщенность мокрой поверхности",60,0,100,5));
    private final ChoiceSetting quality=setting(new ChoiceSetting("wet_quality","Качество отражений","Разрешение и точность мокрых поверхностей. Не влияет на небо","Среднее",java.util.List.of("Низкое","Среднее","Высокое")));
    private final BooleanSetting fog=setting(new BooleanSetting("fog","Туман","Высотный туман с движущимися слоями",true));
    private final NumberSetting density=setting(new NumberSetting("fog_density","Плотность","Насыщенность тумана",40,0,100,1));
    private final NumberSetting height=setting(new NumberSetting("fog_height","Высота","Высота основания тумана",35,-64,192,1));
    private final NumberSetting falloff=setting(new NumberSetting("fog_falloff","Прижатие к земле","Убывание плотности с высотой",35,0,100,1));
    private final NumberSetting haze=setting(new NumberSetting("fog_haze","Дымка неба","Количество дымки вдали",50,0,100,1));
    private final BooleanSetting theme=setting(new BooleanSetting("fog_theme","Цвет клиента","Использовать цвет клиента для тумана",true));
    private final ColorSetting color=setting(new ColorSetting("fog_color","Цвет тумана",0x756CFF));
    public Shaders(){
        super("shaders","Shaders","Шейдерные эффекты мира",ModuleCategory.VISUALS);
        skyCode.visibleWhen(()->false);setting(new ActionSetting("shader_editor","Редактор GLSL"));
        handMode.visibleWhen(hands::value);
        skyMode.visibleWhen(sky::value);skyBrightness.visibleWhen(sky::value);skyTheme.visibleWhen(sky::value);skyColor.visibleWhen(()->sky.value()&&!skyTheme.value());
        for(var setting:new Setting<?>[]{density,height,falloff,haze,theme})setting.visibleWhen(fog::value);
        color.visibleWhen(()->fog.value()&&!theme.value());
        reflection.visibleWhen(wet::value);wetness.visibleWhen(wet::value);quality.visibleWhen(wet::value);
    }
    public boolean wetActive(){return wet.value()&&(reflection.value()>.5||wetness.value()>.5);}
    public float reflection(){return reflection.value().floatValue()/100;}
    public float wetness(){return wetness.value().floatValue()/100;}
    public int reflectionSteps(){return switch(quality.value()){case "Низкое"->14;case "Высокое"->40;default->24;};}
    public int reflectionResolutionDivisor(){return switch(quality.value()){case "Низкое"->3;case "Высокое"->1;default->2;};}
    public boolean fogActive(){return fog.value()&&density.value()>.5;}
    public float fogDensity(){float d=density.value().floatValue()/100;return d*d*.05f+d*.002f;}
    public float fogHeight(){return height.value().floatValue();}
    public float fogFalloff(){return falloff.value().floatValue()*.0009f;}
    public float fogHaze(){return haze.value().floatValue()/100;}
    public int fogColor(){return theme.value()?VelaTheme.PRIMARY:color.value();}
    public static Shaders current(){var s=dev.sqwat.vela.VelaClient.services();return s!=null&&s.modules().byId("shaders") instanceof Shaders a&&a.enabled()?a:null;}
}
