package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.BooleanSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/**
 * Third-person camera that orbits freely while the player keeps its real rotation, so movement direction and the
 * rotation the server receives do not change. Bind it as Hold for Kimiko-style behaviour, or Toggle.
 */
public final class Freelook extends ClientModule {
    private final BooleanSetting thirdPerson=setting(new BooleanSetting("third_person","Камера от третьего лица","Переключать вид сзади на время обзора",true));
    private static Freelook active;
    private float yaw,pitch;
    private CameraType previous;
    private Object level;
    public Freelook(){super("freelook","Freelook","Свободный обзор камерой без поворота персонажа",ModuleCategory.UTILITIES);}

    @Override protected void onEnable(){
        var mc=Minecraft.getInstance();
        if(mc.player==null){setEnabled(false);return;}
        yaw=mc.player.getYRot();pitch=mc.player.getXRot();level=mc.level;
        previous=mc.options.getCameraType();
        if(thirdPerson.value()&&previous.isFirstPerson())mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        active=this;
    }
    @Override protected void onDisable(){
        if(active==this)active=null;
        var mc=Minecraft.getInstance();
        if(previous!=null&&mc.options.getCameraType()!=previous)mc.options.setCameraType(previous);
        previous=null;level=null;
    }
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level!=level)setEnabled(false);
    }
    /** Mouse deltas use the same 0.15 scale and pitch clamp as Entity.turn. */
    public static boolean turn(double yawDelta,double pitchDelta){
        var look=active;if(look==null)return false;
        look.yaw+=(float)yawDelta*.15f;
        look.pitch=Math.clamp(look.pitch+(float)pitchDelta*.15f,-90f,90f);
        return true;
    }
    public static boolean active(){return active!=null;}
    public static float yaw(){return active.yaw;}
    public static float pitch(){return active.pitch;}
}
