package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.sqwat.vela.module.setting.*;
import java.util.List;

/**
 * First-person swing animations. Most modes also hold the item in their own pose between swings. A swing
 * starts with every swing of the local player on a local timeline; the server's echo of it is ignored, so a hit on air
 * or a mob plays as smoothly as mining. The real swing and the attack cooldown are untouched.
 */
public final class SwingAnimation extends ClientModule {
    public static final String STANDARD="Стандарт";
    public static final List<String> MODES=List.of("Взмах","Сдвиг","Выпад","Копьё","Вниз","Желе","Шлепок","Бонк","Круг","Реализм",STANDARD);
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Тип","Пресет анимации взмаха","Взмах",MODES));
    private final NumberSetting strength=setting(new NumberSetting("strength","Сила","Сила анимации взмаха",1,.5,3,.05));
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","Множитель длительности анимации взмаха",1,.5,4,.05));
    private final BooleanSetting target=setting(new BooleanSetting("only_target","Только при наводке","Проигрывать анимацию только когда прицел наведён на существо",false));
    private final BooleanSetting preview=setting(new BooleanSetting("preview","Авто взмах","Локальный предпросмотр взмаха при открытых настройках",false));
    private double started=Double.NEGATIVE_INFINITY;
    private int slashSide=-1;
    private Object world;
    /** Set while the server's echo of a swing is handled: such swings must not restart the animation. */
    public static boolean serverEcho;
    public SwingAnimation(){
        super("swing_animation","Swing Animation","Кастомные анимации взмаха от первого лица",ModuleCategory.VISUALS);
        for(var s:List.of(strength,duration,target,preview))s.visibleWhen(()->!mode.value().equals(STANDARD));
    }
    public boolean previewEnabled(){return preview.value();}
    public boolean customMode(){return enabled()&&!mode.value().equals(STANDARD);}
    public boolean applies(boolean usingItem,boolean editor,boolean aimed){
        return enabled()&&!mode.value().equals(STANDARD)&&!usingItem&&!editor&&(!target.value()||aimed);
    }
    @Override public void onClientTick(){
        var current=net.minecraft.client.Minecraft.getInstance().level;
        if(world!=current){reset();world=current;}
    }
    @Override protected void onDisable(){reset();world=null;}
    public void reset(){started=Double.NEGATIVE_INFINITY;slashSide=-1;}
    private float length(float baseTicks){return Math.max(1,baseTicks)*duration.value().floatValue();}
    /**
     * The local player swung (a click on air, a mob or a block, a placed block). Like vanilla, a swing that is still
     * in its first half is not restarted, so holding the button while mining keeps a steady rhythm.
     */
    public void swingStarted(double ticks,float baseTicks){
        if(serverEcho||!Double.isFinite(ticks))return;
        double elapsed=ticks-started;
        if(elapsed>=0&&elapsed<length(baseTicks)*.5)return;
        started=ticks;slashSide=-slashSide;
    }
    /** Position in this module's swing (0..1, 0 when idle), on its own timeline started by {@link #swingStarted}. */
    public float progress(double ticks,float baseTicks,boolean previewing){
        if(!Double.isFinite(ticks))return 0;
        float length=length(baseTicks);
        if(previewing){
            started=Double.NEGATIVE_INFINITY;
            double cycle=ticks%Math.max(20,length+4);
            return cycle<length?(float)(cycle/length):0;
        }
        double elapsed=ticks-started;
        return elapsed>=0&&elapsed<length?(float)(elapsed/length):0;
    }
    private static void x(PoseStack p,float a){p.mulPose(Axis.XP.rotationDegrees(a));}
    private static void y(PoseStack p,float a){p.mulPose(Axis.YP.rotationDegrees(a));}
    private static void z(PoseStack p,float a){p.mulPose(Axis.ZP.rotationDegrees(a));}
    public static float elastic(float p){if(p<=0)return 0;if(p>=1)return 1;return (float)(Math.pow(2,-10*p)*Math.sin((p*10-.75)*2.0943951023931953)+1);}
    static float back(float p){float c1=1.70158f,c3=c1+1,u=p-1;return 1+c3*u*u*u+c1*u*u;}

    /** Replaces vanilla's swing transform; the pose already holds vanilla's arm position and equip offset. */
    public void apply(PoseStack pose,boolean right,float progress){
        if(!Float.isFinite(progress)||mode.value().equals(STANDARD))return;
        float p=Math.clamp(progress,0,1),side=right?1:-1,s=strength.value().floatValue();
        float sin1=(float)Math.sin(p*p*Math.PI),sin2=(float)Math.sin(Math.sqrt(p)*Math.PI),anim=(float)Math.sin(p*Math.PI);
        switch(mode.value()){
            case "Сдвиг" -> {y(pose,side*90);z(pose,side*-60);x(pose,-90-20*anim*s);}
            case "Выпад" -> {
                pose.translate(0,0,-anim*.2f);y(pose,side*45);y(pose,side*sin1*-20);z(pose,side*sin2*-20);
                x(pose,sin2*-Math.clamp(s*55,55,80));y(pose,side*10);x(pose,-80);y(pose,side*60);
            }
            case "Копьё" -> {pose.translate(0,0,-anim*.2f);pose.scale(1,1,anim*(s*.35f)+1);x(pose,-90);}
            case "Вниз" -> {pose.translate(-anim*.5f,anim*.25f,0);y(pose,side*65);x(pose,-110-80*anim);}
            case "Желе" -> {pose.translate(0,0,-anim*.2f);y(pose,side*35);pose.scale(1,1,anim*(p*.65f*s*.2f)+1);x(pose,-90);}
            case "Шлепок" -> {x(pose,-sin2*55);x(pose,sin2*sin1*-120*s);y(pose,side*sin2*60);x(pose,sin2*55);}
            case "Бонк" -> {y(pose,15*side);x(pose,-95*elastic(p*p));}
            case "Круг" -> {
                y(pose,15*side);
                float mix=p+(1-p)*(p*.5f-p);
                x(pose,-(360*back(mix)+45));
            }
            case "Реализм" -> {
                float tilt=side*slashSide*22*anim*Math.clamp(s,.5f,2);
                pose.scale(1,1,anim+1);z(pose,tilt);x(pose,-90*anim*(s*.3f));x(pose,-45*elastic(p*p)*anim);
            }
            default -> {
                // "Взмах"
                y(pose,side*(45+sin1*-20));z(pose,side*sin2*-20);x(pose,sin2*-s*22);y(pose,side*-45);
            }
        }
    }
    /** Vanilla's swing length: the held item's swing duration, shortened by Haste, lengthened by Mining Fatigue. */
    public static float swingTicks(net.minecraft.world.entity.player.Player player){
        float ticks=Math.max(1,player.getMainHandItem().getSwingAnimation().duration());
        var haste=player.getEffect(net.minecraft.world.effect.MobEffects.HASTE);
        var fatigue=player.getEffect(net.minecraft.world.effect.MobEffects.MINING_FATIGUE);
        if(haste!=null)return Math.max(1,ticks-(1+haste.getAmplifier()));
        if(fatigue!=null)return ticks+(1+fatigue.getAmplifier())*2;
        return ticks;
    }
}
