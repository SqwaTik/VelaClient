package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ChoiceSetting;
import dev.sqwat.vela.module.setting.ColorSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.ui.render.VelaDraw;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * GTA-style death: the vanilla death screen is held back while the frame darkens, a caption scales in and the camera
 * optionally orbits the body. Afterwards the held screen opens, or the normal respawn action runs.
 */
public final class Wasted extends ClientModule {
    private final ChoiceSetting caption=setting(new ChoiceSetting("caption","Надпись","Текст после смерти","ПОТРАЧЕНО",java.util.List.of("ПОТРАЧЕНО","WASTED","ВЫ ПОГИБЛИ")));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет надписи",0xC8102E));
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","Время эффекта, мс",3000,1000,6000,250));
    private final BooleanSetting orbit=setting(new BooleanSetting("orbit","Облёт камеры","Камера медленно облетает место смерти",true));
    private final NumberSetting orbitSpeed=setting(new NumberSetting("orbit_speed","Скорость облёта","Градусов в секунду",25,5,90,5));
    private final BooleanSetting sound=setting(new BooleanSetting("sound","Звук смерти","Глухой удар в начале эффекта",true));
    private final BooleanSetting respawn=setting(new BooleanSetting("respawn","Возрождать после эффекта","Вместо экрана смерти сразу возрождаться",false));
    private static Wasted running;
    private Screen pending;
    private long started;
    private float startYaw;
    private CameraType previousCamera;
    private Object level;

    public Wasted(){
        super("wasted","Wasted","Кинематографичная смерть в стиле GTA",ModuleCategory.VISUALS);
        orbitSpeed.visibleWhen(orbit::value);
    }

    /** Called instead of opening the death screen. Returns false when the module does not take over. */
    public static boolean intercept(Screen deathScreen){
        var s=dev.sqwat.vela.VelaClient.services();var mc=Minecraft.getInstance();
        if(s==null||!(s.modules().byId("wasted") instanceof Wasted module)||!module.enabled()||mc.player==null)return false;
        module.begin(deathScreen,mc);
        return true;
    }
    private void begin(Screen deathScreen,Minecraft mc){
        pending=deathScreen;started=System.nanoTime();level=mc.level;startYaw=mc.player.getYRot();running=this;
        if(orbit.value()){previousCamera=mc.options.getCameraType();mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
        if(sound.value())mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(),.5f,.9f));
    }
    @Override public void onClientTick(){
        if(running!=this)return;
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level!=level||!mc.player.isDeadOrDying()){finish(mc,false);return;}
        if(elapsed()>=duration.value().longValue()*1_000_000L)finish(mc,true);
    }
    @Override protected void onDisable(){if(running==this)finish(Minecraft.getInstance(),true);}
    /** {@code deathContinues}: still dead in the same world, so the held screen or a respawn follows. */
    private void finish(Minecraft mc,boolean deathContinues){
        running=null;
        if(previousCamera!=null){mc.options.setCameraType(previousCamera);previousCamera=null;}
        var screen=pending;pending=null;level=null;
        if(!deathContinues||mc.player==null)return;
        if(respawn.value()&&mc.player.isDeadOrDying())mc.player.respawn();
        else if(screen!=null&&mc.gui.screen()==null)mc.gui.setScreen(screen);
    }
    private long elapsed(){return System.nanoTime()-started;}

    /** Camera yaw while orbiting, or NaN when the vanilla rotation applies. */
    public static float orbitYaw(){
        var w=running;if(w==null||!w.orbit.value())return Float.NaN;
        return w.startYaw+w.elapsed()/1e9f*w.orbitSpeed.value().floatValue();
    }
    public static float orbitPitch(){return 28;}

    /** Full-screen darkening, edge vignette and the caption, drawn with the HUD. */
    public static void draw(GuiGraphicsExtractor g){
        var w=running;if(w==null)return;
        float t=w.elapsed()/1e9f,total=w.duration.value().floatValue()/1000;
        float k=Math.clamp(t/.6f,0,1)*Math.clamp((total-t)/.35f,0,1);
        int width=g.guiWidth(),height=g.guiHeight();
        g.fill(0,0,width,height,(Math.round(150*k)<<24)|0x101010);
        int edge=Math.max(24,height/4);
        g.fillGradient(0,0,width,edge,Math.round(170*k)<<24,0);
        g.fillGradient(0,height-edge,width,height,0,Math.round(170*k)<<24);
        float reveal=Math.clamp((t-.35f)/.45f,0,1);
        if(reveal<=0)return;
        float ease=1-(1-reveal)*(1-reveal)*(1-reveal);
        float size=28*(1.45f-.45f*ease);
        int alpha=Math.round(255*ease*Math.clamp((total-t)/.35f,0,1));
        String text=w.caption.value();
        g.pose().pushMatrix();
        try{
            g.pose().translate(width/2f,height/2f-size*.6f);
            VelaDraw.centeredText(g,text,1.5f,1.5f,Math.round(alpha*.7f)<<24,VelaDraw.Weight.SEMIBOLD,size);
            VelaDraw.centeredText(g,text,0,0,(alpha<<24)|(w.color.value()&0xFFFFFF),VelaDraw.Weight.SEMIBOLD,size);
        }finally{g.pose().popMatrix();}
    }
}
