package dev.sqwat.vela.module.utility;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.LightmapRenderState;

public final class Gamma extends ClientModule {
    private final BooleanSetting smart=setting(new BooleanSetting("smart","Умный","Плавное усиление яркости только в темноте",true));
    private float amount;
    public Gamma(){super("gamma","Gamma","Повышение яркости тёмных участков",ModuleCategory.UTILITIES);}
    public static float target(boolean smart,int light){return smart?Math.clamp((9-light)/9f,0,.95f):1f;}
    @Override protected void onDisable(){amount=0;}
    @Override public void onClientTick(){
        var c=Minecraft.getInstance();if(c.level==null||c.player==null){amount=0;return;}
        int light=c.level.getMaxLocalRawBrightness(c.gameRenderer.mainCamera().blockPosition());
        amount=dev.sqwat.vela.ui.animation.Motion.approach(amount,target(smart.value(),light),4,.05f);
    }
    public static void apply(LightmapRenderState state){
        var s=dev.sqwat.vela.VelaClient.services();
        if(s==null||!(s.modules().byId("gamma") instanceof Gamma m)||!m.enabled())return;
        var c=Minecraft.getInstance();if(c.level==null||c.player==null)return;
        state.nightVisionEffectIntensity=Math.max(state.nightVisionEffectIntensity,m.amount);
        state.brightness=Math.max(state.brightness,m.amount);
    }
}
