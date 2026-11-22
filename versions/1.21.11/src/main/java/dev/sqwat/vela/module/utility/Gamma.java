package dev.sqwat.vela.module.utility;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

/** 1.21.11: no lightmap render state, so GammaMixin raises the night vision and brightness that LightTexture writes. */
public final class Gamma extends ClientModule {
    private final BooleanSetting smart=setting(new BooleanSetting("smart","Умный","Плавное усиление яркости только в темноте",true));
    private float amount;
    public Gamma(){super("gamma","Gamma","Повышение яркости тёмных участков",ModuleCategory.UTILITIES);}
    public static float target(boolean smart,int light){return smart?Math.clamp((9-light)/9f,0,.95f):1f;}
    @Override protected void onDisable(){amount=0;}
    @Override public void onClientTick(){
        var c=Minecraft.getInstance();if(c.level==null||c.player==null){amount=0;return;}
        int light=c.level.getMaxLocalRawBrightness(c.gameRenderer.getMainCamera().blockPosition());
        amount=dev.sqwat.vela.ui.animation.Motion.approach(amount,target(smart.value(),light),4,.05f);
    }
    /** The value at least as bright as the module asks for (night vision intensity, brightness). */
    public static float boost(float value){
        var s=dev.sqwat.vela.VelaClient.services();
        if(s==null||!(s.modules().byId("gamma") instanceof Gamma m)||!m.enabled())return value;
        var c=Minecraft.getInstance();if(c.level==null||c.player==null)return value;
        return Math.max(value,m.amount);
    }
}
