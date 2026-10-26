package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.sqwat.vela.module.setting.*;
import java.util.List;

/** Local render timeline: does not change the player's swing state or attack cooldown. */
public final class SwingAnimations extends ClientModule {
    public static final List<String> MODES=List.of("Взмах","Взмах 2","Сдвиг","Слом","Выпад","Копьё","Вниз","Желе","Шлепок","Бонк","Круг","Реализм","Стандарт");
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Режим","Движение предмета при взмахе","Взмах",MODES));
    private final NumberSetting strength=setting(new NumberSetting("strength","Сила","Амплитуда движения",1,.5,3,.05));
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","Множитель длительности визуального взмаха",1,.5,4,.05));
    private final BooleanSetting target=setting(new BooleanSetting("only_target","Только при наводке","Анимация при наведении на живую цель",false));
    private final BooleanSetting preview=setting(new BooleanSetting("preview","Авто взмах","Локальный предпросмотр при открытых настройках",false));
    private final BooleanSetting swords=setting(new BooleanSetting("swords_only","Только мечи","Ограничить эффект мечами",false));
    private double started=Double.NEGATIVE_INFINITY;
    private float previousProgress;
    private int slashSide=-1;
    private Object world;
    public SwingAnimations(){
        super("swing_animations","Swing Animations","Анимации взмаха от первого лица",ModuleCategory.VISUALS);
        for(var s:List.of(strength,duration,target,preview,swords))s.visibleWhen(()->!mode.value().equals("Стандарт"));
    }
    public boolean previewEnabled(){return preview.value();}
    public boolean applies(boolean sword,boolean usingItem,boolean editor){return applies(sword,usingItem,editor,true);}
    public boolean applies(boolean sword,boolean usingItem,boolean editor,boolean aimed){
        return enabled()&&!mode.value().equals("Стандарт")&&(!swords.value()||sword)&&!usingItem&&!editor&&(!target.value()||aimed);
    }
    @Override public void onClientTick(){
        var current=net.minecraft.client.Minecraft.getInstance().level;
        if(world!=current){reset();world=current;}
    }
    @Override protected void onDisable(){reset();world=null;}
    public void reset(){started=Double.NEGATIVE_INFINITY;previousProgress=0;slashSide=-1;}
    public float progress(float nativeProgress,double ticks,float baseTicks,boolean previewing){
        if(!Float.isFinite(nativeProgress)||!Double.isFinite(ticks))return 0;
        float length=Math.max(1,baseTicks)*duration.value().floatValue();
        if(previewing){
            previousProgress=0;started=Double.NEGATIVE_INFINITY;
            double cycle=ticks%Math.max(20,length+4);
            return cycle<length?(float)(cycle/length):0;
        }
        if(nativeProgress>0&&(previousProgress<=0||nativeProgress<previousProgress-.25f)){
            started=ticks-nativeProgress*Math.max(1,baseTicks);slashSide=-slashSide;
        }
        previousProgress=nativeProgress;
        double elapsed=ticks-started;
        return elapsed>=0&&elapsed<length?(float)(elapsed/length):0;
    }
    private static void x(PoseStack p,float a){p.mulPose(Axis.XP.rotationDegrees(a));}
    private static void y(PoseStack p,float a){p.mulPose(Axis.YP.rotationDegrees(a));}
    private static void z(PoseStack p,float a){p.mulPose(Axis.ZP.rotationDegrees(a));}
    public static float elastic(float p){if(p<=0)return 0;if(p>=1)return 1;return (float)(Math.pow(2,-10*p)*Math.sin((p*10-.75)*2.0943951023931953)+1);}
    private static float back(float p){float t=p-1;return 1+2.70158f*t*t*t+1.70158f*t*t;}
    public void apply(PoseStack pose,boolean right,float progress){
        if(!Float.isFinite(progress)||mode.value().equals("Стандарт"))return;
        float p=Math.clamp(progress,0,1),side=right?1:-1,s=strength.value().floatValue();
        float a=(float)Math.sin(p*Math.PI),b=(float)Math.sin(p*p*Math.PI),c=(float)Math.sin(Math.sqrt(p)*Math.PI);
        switch(mode.value()){
            case "Взмах 2" -> {pose.scale(1,1,1+a);x(pose,-27*a*s);}
            case "Сдвиг" -> {y(pose,side*90);z(pose,-side*60);x(pose,-90-20*a*s);}
            case "Слом" -> {y(pose,side*90);z(pose,-side*30);x(pose,-90-20*a*s);}
            case "Выпад" -> {pose.translate(0,0,-a*.2f);y(pose,side*45);y(pose,-side*b*20);z(pose,-side*c*20);x(pose,-c*Math.clamp(s*55,55,80));y(pose,side*10);x(pose,-80);y(pose,side*60);}
            case "Копьё" -> {pose.translate(0,0,-a*.2f);pose.scale(1,1,1+a*s*.35f);x(pose,-90);}
            case "Вниз" -> {pose.translate(-side*a*.5f,a*.25f,0);y(pose,side*65);x(pose,-110-80*a);}
            case "Желе" -> {pose.translate(0,0,-a*.2f);y(pose,side*35);pose.scale(1,1,1+a*p*.13f*s);x(pose,-90);}
            case "Шлепок" -> {x(pose,-c*55);x(pose,-c*b*120*s);y(pose,side*c*60);x(pose,c*55);}
            case "Бонк" -> {y(pose,side*15);x(pose,-95*elastic(p*p));}
            case "Круг" -> {y(pose,side*15);float t=p+(p*.5f-p)*(1-p);x(pose,-360*back(t)-45);}
            case "Реализм" -> {pose.scale(1,1,1+a);z(pose,side*slashSide*22*a*Math.clamp(s,.5f,2));x(pose,-27*a*s);x(pose,-45*elastic(p*p)*a);}
            default -> {y(pose,side*(45-b*20));z(pose,-side*c*20);x(pose,-c*s*22);y(pose,-side*45);}
        }
    }
}
