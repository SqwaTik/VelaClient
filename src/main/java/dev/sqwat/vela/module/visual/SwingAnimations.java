package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.sqwat.vela.module.setting.*;
import java.util.List;

/**
 * First-person swing animations. Each mode is one motion with one peak over the swing, so nothing plays twice. A
 * swing starts with every vanilla swing (hits and swings at the air alike); a critical hit (swinging while falling)
 * moves 40% further and lasts 15% longer. Local render timeline only: the player's real swing and attack cooldown
 * are untouched.
 */
public final class SwingAnimations extends ClientModule {
    public static final String STANDARD="Стандарт";
    public static final List<String> MODES=List.of("Плавный","Рубящий","Боковой","Выпад","Апперкот","Вращение","Шлепок","Блок","Пружина",STANDARD);
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Режим","Движение предмета при взмахе","Плавный",MODES));
    private final NumberSetting strength=setting(new NumberSetting("strength","Сила","Амплитуда движения",1.2,.5,3,.05));
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","Множитель длительности визуального взмаха",1,.5,3,.05));
    private final BooleanSetting crits=setting(new BooleanSetting("crits","Сильнее на критах","Критический удар даёт более широкий и долгий взмах",true));
    private final BooleanSetting target=setting(new BooleanSetting("only_target","Только при наводке","Анимация при наведении на живую цель",false));
    private final BooleanSetting preview=setting(new BooleanSetting("preview","Авто взмах","Локальный предпросмотр при открытых настройках",false));
    private final BooleanSetting swords=setting(new BooleanSetting("swords_only","Только мечи","Ограничить эффект мечами",false));
    private double started=Double.NEGATIVE_INFINITY;
    private float previousProgress;
    private int slashSide=-1;
    private boolean crit;
    private Object world;
    public SwingAnimations(){
        super("swing_animations","Swing Animations","Анимации взмаха от первого лица",ModuleCategory.VISUALS);
        for(var s:List.of(strength,duration,crits,target,preview,swords))s.visibleWhen(()->!mode.value().equals(STANDARD));
    }
    public boolean previewEnabled(){return preview.value();}
    public boolean applies(boolean sword,boolean usingItem,boolean editor){return applies(sword,usingItem,editor,true);}
    public boolean applies(boolean sword,boolean usingItem,boolean editor,boolean aimed){
        return enabled()&&!mode.value().equals(STANDARD)&&(!swords.value()||sword)&&!usingItem&&!editor&&(!target.value()||aimed);
    }
    @Override public void onClientTick(){
        var current=net.minecraft.client.Minecraft.getInstance().level;
        if(world!=current){reset();world=current;}
    }
    @Override protected void onDisable(){reset();world=null;}
    public void reset(){started=Double.NEGATIVE_INFINITY;previousProgress=0;slashSide=-1;crit=false;}
    private float length(float baseTicks){return Math.max(1,baseTicks)*duration.value().floatValue()*(crit?1.15f:1);}
    /**
     * Position in this module's swing (0..1, 0 when idle). A new swing starts whenever vanilla's progress starts
     * again: from rest, or restarted halfway by a quick second click.
     */
    public float progress(float nativeProgress,double ticks,float baseTicks,boolean previewing){return progress(nativeProgress,ticks,baseTicks,previewing,false);}
    public float progress(float nativeProgress,double ticks,float baseTicks,boolean previewing,boolean critical){
        if(!Float.isFinite(nativeProgress)||!Double.isFinite(ticks))return 0;
        if(previewing){
            previousProgress=0;started=Double.NEGATIVE_INFINITY;crit=false;
            float length=length(baseTicks);
            double cycle=ticks%Math.max(20,length+4);
            return cycle<length?(float)(cycle/length):0;
        }
        if(nativeProgress>0&&(previousProgress<=0||nativeProgress<previousProgress-.2f)){
            started=ticks-nativeProgress*Math.max(1,baseTicks);slashSide=-slashSide;crit=critical&&crits.value();
        }
        previousProgress=nativeProgress;
        double elapsed=ticks-started;float length=length(baseTicks);
        return elapsed>=0&&elapsed<length?(float)(elapsed/length):0;
    }
    private static void x(PoseStack p,float a){p.mulPose(Axis.XP.rotationDegrees(a));}
    private static void y(PoseStack p,float a){p.mulPose(Axis.YP.rotationDegrees(a));}
    private static void z(PoseStack p,float a){p.mulPose(Axis.ZP.rotationDegrees(a));}
    /** Fast out, soft return: rises to 1 by ~35% of the swing, back to 0 at the end. One peak. */
    static float strike(float p){
        if(p<=0||p>=1)return 0;
        return p<.35f?ease(p/.35f):1-smooth((p-.35f)/.65f);
    }
    private static float ease(float t){float u=1-t;return 1-u*u*u;}
    private static float smooth(float t){t=Math.clamp(t,0,1);return t*t*(3-2*t);}
    /** One damped bounce: down, a small rebound, settle; never a second full hit. */
    static float bounce(float p){
        if(p<=0||p>=1)return 0;
        float e=strike(p);
        return e+(float)(Math.sin(p*Math.PI*3)*.12*(1-p)*(p>.35f?1:0));
    }
    public static float elastic(float p){if(p<=0)return 0;if(p>=1)return 1;return (float)(Math.pow(2,-10*p)*Math.sin((p*10-.75)*2.0943951023931953)+1);}

    public void apply(PoseStack pose,boolean right,float progress){
        if(!Float.isFinite(progress)||mode.value().equals(STANDARD))return;
        float p=Math.clamp(progress,0,1),side=right?1:-1;
        float s=strength.value().floatValue()*(crit?1.4f:1);
        float e=strike(p);
        switch(mode.value()){
            case "Рубящий" -> {pose.translate(0,e*.08f,-e*.12f);y(pose,side*20*e);x(pose,-e*100*s);}
            case "Боковой" -> {float dir=side*slashSide;pose.translate(-dir*e*.18f,0,-e*.1f);y(pose,dir*e*(40-57*s));z(pose,-dir*e*25);x(pose,-e*30);}
            case "Выпад" -> {pose.translate(0,-e*.05f,-e*.45f*s);x(pose,-10*e);}
            case "Апперкот" -> {pose.translate(0,-e*.1f,-e*.1f);y(pose,side*10*e);x(pose,-e*(95*s*(p>.35f?1:p/.35f)-45*s+10));}
            case "Вращение" -> {y(pose,side*15*e);x(pose,-360*ease(Math.min(1,p*1.15f))-20*e);}
            case "Шлепок" -> {y(pose,side*80*e);z(pose,-side*20*e);x(pose,-e*85*s);}
            case "Блок" -> {pose.translate(-side*.1f*e,.08f*e,0);y(pose,side*70*e);z(pose,-side*60*e);x(pose,-e*(80+40*s));}
            case "Пружина" -> {y(pose,side*15*e);x(pose,-bounce(p)*95*s);}
            default -> {
                // "Плавный": vanilla's swing shape, stronger and with a smooth return.
                pose.translate(-side*.2f*e,.06f*e,-.12f*e);
                y(pose,side*(45-e*28*s));z(pose,-side*e*24*s);x(pose,-e*s*75);y(pose,-side*45);
            }
        }
    }
}
