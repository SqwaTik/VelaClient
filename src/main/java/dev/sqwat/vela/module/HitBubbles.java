package dev.sqwat.vela.module;

import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;
import java.util.List;

/** Bounded world-local refractive waves, emitted by the normal local attack callback. */
public final class HitBubbles extends ClientModule {
    public static final int LIMIT=16;
    public record Wave(Vec3 position,long born,long lifetime){
        public float progress(long now){return Math.clamp((float)((double)(now-born)/lifetime),0,1);}
        public float envelope(long now){float p=progress(now);return Math.min(1,p/.1f)*Math.min(1,(1-p)/.3f);}
    }
    private final ArrayDeque<Wave> waves=new ArrayDeque<>(LIMIT);
    private Object world;
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Режим","Вид эффекта удара","Пузырь",List.of("Пузырь","Ударная волна")));
    private final NumberSetting waveRadius=setting(new NumberSetting("wave_radius","Радиус волны","Размер стеклянной волны в блоках",1.2,.5,3,.1));
    private final dev.sqwat.vela.render.GlassWave glassWave=new dev.sqwat.vela.render.GlassWave();
    public boolean shockwave(){return mode.value().equals("Ударная волна");}
    private final NumberSetting strength=setting(new NumberSetting("strength","Сила","Сила преломления волны",1.5,.2,3,.1));
    private final NumberSetting size=setting(new NumberSetting("size","Размер","Радиус волны относительно высоты экрана",.2,.1,.6,.05));
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","Время жизни волны в миллисекундах",1000,300,2000,50));
    private final BooleanSetting warp=setting(new BooleanSetting("warp","Искривление","Волнистое искажение внутри пузыря",false));
    private final NumberSetting warpStrength=setting(new NumberSetting("warp_strength","Сила искривления","Интенсивность внутренней волны",.5,.1,2,.1));
    private final NumberSetting saturation=setting(new NumberSetting("saturation","Насыщенность","Насыщенность изображения внутри пузыря",1,-1,1,.05));
    private final BooleanSetting tint=setting(new BooleanSetting("tint","Подкрашивать цветом","Окрашивать гребень волны",true));
    private final NumberSetting tintStrength=setting(new NumberSetting("tint_strength","Сила цвета","Интенсивность окрашивания в процентах",100,0,100,1));
    private final ChoiceSetting colorMode=setting(new ChoiceSetting("color_mode","Режим цвета","Палитра волны","Клиент",List.of("Радуга","Клиент","Свой")));
    private final BooleanSetting second=setting(new BooleanSetting("second_color","Второй цвет","Градиент между двумя цветами",false));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0xFFFFFF));
    private final ColorSetting color2=setting(new ColorSetting("color2","Цвет 2",0x585858));
    public HitBubbles(){
        super("hit_bubbles","Hit Bubbles","Преломляющая волна в месте удара",ModuleCategory.VISUALS);
        waveRadius.visibleWhen(this::shockwave);size.visibleWhen(()->!shockwave());strength.visibleWhen(()->!shockwave());warp.visibleWhen(()->!shockwave());saturation.visibleWhen(()->!shockwave());
        warpStrength.visibleWhen(()->!shockwave()&&warp.value());tintStrength.visibleWhen(()->!shockwave()&&tint.value());colorMode.visibleWhen(()->!shockwave()&&tint.value());
        tint.visibleWhen(()->!shockwave());
        second.visibleWhen(()->!shockwave()&&tint.value()&&colorMode.value().equals("Свой"));
        color.visibleWhen(()->!shockwave()&&tint.value()&&colorMode.value().equals("Свой"));
        color2.visibleWhen(()->!shockwave()&&color.visible()&&second.value());
    }
    public static void attacked(Entity entity){
        var s=dev.sqwat.vela.VelaClient.services();
        if(s==null||!(s.modules().byId("hit_bubbles") instanceof HitBubbles m)||!m.enabled())return;
        if(m.world!=entity.level()){m.waves.clear();m.world=entity.level();}
        m.spawn(entity.position().add(0,entity.getBbHeight()*.5,0),System.nanoTime());
    }
    public void spawn(Vec3 position,long now){
        if(!enabled())return;
        expire(now);int limit=shockwave()?dev.sqwat.vela.render.JumpWaveShape.MAX_WAVES:LIMIT;
        while(waves.size()>=limit)waves.removeFirst();
        waves.addLast(new Wave(position,now,(long)(duration.value()*1_000_000)));
    }
    public Iterable<Wave> waves(){return waves;}
    public int count(){return waves.size();}
    public void expire(long now){waves.removeIf(w->now-w.born()>=w.lifetime());}
    @Override protected void onDisable(){waves.clear();world=null;}
    @Override public void onClientTick(){var level=Minecraft.getInstance().level;if(world!=level){waves.clear();world=level;}expire(System.nanoTime());}
    public float radius(){return size.value().floatValue();}
    public void draw(net.minecraft.client.gui.GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();if(!enabled()||!shockwave()||world!=mc.level||mc.level==null||!dev.sqwat.vela.ui.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();expire(now);
        var rotation=mc.gameRenderer.mainCamera().rotation();
        var x=new org.joml.Vector3f(1,0,0).rotate(rotation);var y=new org.joml.Vector3f(0,1,0).rotate(rotation);
        var axisX=new Vec3(x.x,x.y,x.z);var axisY=new Vec3(y.x,y.y,y.z);
        g.pose().pushMatrix();g.pose().identity();
        try{int count=0;for(var wave:waves){if(count++>=dev.sqwat.vela.render.JumpWaveShape.MAX_WAVES)break;
            double t=wave.progress(now),radius=dev.sqwat.vela.render.JumpWaveShape.radius(t,waveRadius.value());
            int tint=dev.sqwat.vela.render.NeonGeometry.tint(color(now,false),dev.sqwat.vela.render.JumpWaveShape.opacity(t)*.96);
            glassWave.draw(g,wave.position(),axisX,axisY,radius,tint);
        }}finally{g.pose().popMatrix();}
    }
    public float strength(){return strength.value().floatValue();}
    public float warp(){return warp.value()?warpStrength.value().floatValue()*.01f:0;}
    public float saturation(){return 1+saturation.value().floatValue();}
    public float tint(){return tint.value()?tintStrength.value().floatValue()/100:0;}
    public int color(long now,boolean alternate){
        if(shockwave())return VelaTheme.PRIMARY;
        if(colorMode.value().equals("Радуга"))return java.awt.Color.HSBtoRGB((float)((now%8_000_000_000L)/8e9+(alternate?.2:0)),.75f,1);
        if(colorMode.value().equals("Клиент"))return alternate?VelaTheme.mix(VelaTheme.PRIMARY,0xFFFFFFFF,.35f):VelaTheme.PRIMARY;
        return alternate&&second.value()?color2.value():color.value();
    }
}
