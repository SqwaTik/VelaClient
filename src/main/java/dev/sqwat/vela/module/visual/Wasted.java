package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ChoiceSetting;
import dev.sqwat.vela.module.setting.ColorSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.ui.render.VelaDraw;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * GTA-style death in two phases. Effect: the vanilla death screen is held back while the frame darkens, the caption
 * scales in, the camera can orbit the body and Vela's own death sound plays. Screen: the held death screen opens in
 * the same look (no red gradient or vanilla title, the cause of death under the caption) and its buttons fade in.
 * With "respawn after effect" the normal respawn action runs instead of the screen.
 */
public final class Wasted extends ClientModule {
    private static final SoundEvent SOUND=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("velaclient","death.wasted"));
    private static final long BUTTON_FADE=600_000_000L;
    private final ChoiceSetting caption=setting(new ChoiceSetting("caption","Надпись","Текст после смерти","ПОТРАЧЕНО",java.util.List.of("ПОТРАЧЕНО","WASTED","ВЫ ПОГИБЛИ")));
    private final BooleanSetting clientColor=setting(new BooleanSetting("client_color","Цвет клиента","Надпись в цвете темы Vela",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет надписи",0xC8102E));
    {color.visibleWhen(()->!clientColor.value());}
    private int textColor(){return (clientColor.value()?dev.sqwat.vela.ui.render.VelaTheme.PRIMARY:color.value())&0xFFFFFF;}
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","Время эффекта до появления кнопок, мс",3000,1000,6000,250));
    private final BooleanSetting orbit=setting(new BooleanSetting("orbit","Облёт камеры","Камера медленно облетает место смерти",true));
    private final NumberSetting orbitSpeed=setting(new NumberSetting("orbit_speed","Скорость облёта","Градусов в секунду",25,5,90,5));
    private final BooleanSetting sound=setting(new BooleanSetting("sound","Звук смерти","Кинематографичный звук Vela",true));
    private final NumberSetting volume=setting(new NumberSetting("volume","Громкость","Громкость звука смерти",80,10,100,5));
    private final BooleanSetting respawn=setting(new BooleanSetting("respawn","Возрождать после эффекта","Вместо экрана смерти сразу возрождаться",false));
    private static Wasted running,presenting;
    private Screen pending;
    private long started,screenOpened;
    private float startYaw;
    private CameraType previousCamera;
    private Object level;

    public Wasted(){
        super("wasted","Wasted","Кинематографичная смерть в стиле GTA",ModuleCategory.VISUALS);
        orbitSpeed.visibleWhen(orbit::value);volume.visibleWhen(sound::value);
    }

    /** Called instead of opening the death screen. Returns false when the module does not take over. */
    public static boolean intercept(Screen deathScreen){
        var s=dev.sqwat.vela.VelaClient.services();var mc=Minecraft.getInstance();
        if(s==null||!(s.modules().byId("wasted") instanceof Wasted module)||!module.enabled()||mc.player==null)return false;
        module.begin(deathScreen,mc);return true;
    }
    public static boolean effectRunning(){return running!=null;}
    /** A death screen opened while the effect runs is held (replacing the earlier one) instead of shown. */
    public static boolean hold(DeathScreen screen){
        var w=running;if(w==null)return false;
        w.pending=screen;return true;
    }
    private void begin(Screen deathScreen,Minecraft mc){
        pending=deathScreen;started=System.nanoTime();level=mc.level;startYaw=mc.player.getYRot();running=this;presenting=null;
        if(orbit.value()){previousCamera=mc.options.getCameraType();mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
        if(sound.value())mc.getSoundManager().play(SimpleSoundInstance.forUI(SOUND,1f,volume.value().floatValue()/100));
    }
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(presenting==this&&!(mc.gui.screen() instanceof DeathScreen))presenting=null;
        if(running!=this)return;
        if(mc.player==null||mc.level!=level||!mc.player.isDeadOrDying()){finish(mc,false);return;}
        if(elapsed()>=duration.value().longValue()*1_000_000L)finish(mc,true);
    }
    @Override protected void onDisable(){if(running==this)finish(Minecraft.getInstance(),true);if(presenting==this)presenting=null;}
    /** {@code deathContinues}: still dead in the same world, so the held screen or a respawn follows. */
    private void finish(Minecraft mc,boolean deathContinues){
        running=null;
        if(previousCamera!=null){mc.options.setCameraType(previousCamera);previousCamera=null;}
        var screen=pending;pending=null;level=null;
        if(!deathContinues||mc.player==null)return;
        if(respawn.value()&&mc.player.isDeadOrDying()){mc.player.respawn();return;}
        // Always end on a death screen, even if chat is open: without one the dead player would have no buttons.
        presenting=this;screenOpened=System.nanoTime();
        mc.gui.setScreen(screen); // null: vanilla builds the death screen itself now that the effect is over
    }
    private long elapsed(){return System.nanoTime()-started;}

    /** Camera yaw while orbiting, or NaN when the vanilla rotation applies. */
    public static float orbitYaw(){
        var w=running;if(w==null||!w.orbit.value())return Float.NaN;
        return w.startYaw+w.elapsed()/1e9f*w.orbitSpeed.value().floatValue();
    }
    public static float orbitPitch(){return 28;}

    /** Effect phase, drawn with the HUD. The fade-out happens only when no death screen follows. */
    public static void draw(GuiGraphicsExtractor g){
        var w=running;if(w==null)return;
        float t=w.elapsed()/1e9f,total=w.duration.value().floatValue()/1000;
        float out=w.respawn.value()?Math.clamp((total-t)/.35f,0,1):1;
        float reveal=Math.clamp((t-.35f)/.45f,0,1);
        w.paint(g,Math.clamp(t/.6f,0,1)*out,1-(1-reveal)*(1-reveal)*(1-reveal),out,null);
    }
    /** Screen phase: replaces the death screen background and title. */
    public static boolean drawScreen(GuiGraphicsExtractor g,net.minecraft.network.chat.Component cause){
        var w=presenting;if(w==null)return false;
        w.paint(g,1,1,1,cause);return true;
    }
    /** Button opacity on the death screen: they fade in once the screen opens. */
    public static float buttonAlpha(){
        var w=presenting;if(w==null)return 1;
        float a=Math.clamp((System.nanoTime()-w.screenOpened)/(float)BUTTON_FADE,0,1);
        return a*a*(3-2*a);
    }
    public static boolean presenting(){return presenting!=null;}
    /** First button row under the centred caption and the cause line, in GUI pixels. */
    public static int buttonTop(int height){return Math.round(height/2f+28*.55f+26);}

    private void paint(GuiGraphicsExtractor g,float darkness,float ease,float alphaScale,net.minecraft.network.chat.Component cause){
        int width=g.guiWidth(),height=g.guiHeight();
        g.fill(0,0,width,height,(Math.round(150*darkness)<<24)|0x101010);
        int edge=Math.max(24,height/4);
        g.fillGradient(0,0,width,edge,Math.round(170*darkness)<<24,0);
        g.fillGradient(0,height-edge,width,height,0,Math.round(170*darkness)<<24);
        if(ease<=0)return;
        float size=28*(1.45f-.45f*ease);int alpha=Math.round(255*ease*alphaScale);
        String text=caption.value();
        // Centred on screen; the scale-in keeps the same centre. Death screen buttons are moved below (buttonTop).
        float y=height/2f-size*.45f;
        g.pose().pushMatrix();
        try{
            g.pose().translate(width/2f,y);
            VelaDraw.centeredText(g,text,1.5f,1.5f,Math.round(alpha*.7f)<<24,VelaDraw.Weight.SEMIBOLD,size);
            VelaDraw.centeredText(g,text,0,0,(alpha<<24)|textColor(),VelaDraw.Weight.SEMIBOLD,size);
            if(cause!=null){
                String reason=VelaDraw.ellipsize(cause.getString(),width-40,VelaDraw.Weight.REGULAR,9);
                if(!reason.isEmpty())VelaDraw.centeredText(g,reason,0,size+8,(Math.round(alpha*.85f)<<24)|(VelaTheme.TEXT_SECONDARY&0xFFFFFF),VelaDraw.Weight.REGULAR,9);
            }
        }finally{g.pose().popMatrix();}
    }
}
