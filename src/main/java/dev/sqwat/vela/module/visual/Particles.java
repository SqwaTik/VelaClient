package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.render.effect.HitParticles;
import dev.sqwat.vela.render.effect.ParticleAtlas;
import dev.sqwat.vela.render.effect.NeonGeometry;
import dev.sqwat.vela.render.effect.WorldSprite;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import java.util.List;
import java.util.Random;

public final class Particles extends ClientModule {
    private static final Identifier ATLAS=Identifier.fromNamespaceAndPath("velaclient","textures/effects/particles/atlas.png");
    private static final List<String> SHAPES=List.of("Все","Точка","Звезда","Молния","Крест","Корона","Сердце","Линия","Ромб","Доллар","Снежинка","Треугольник");
    private final NumberSetting count=setting(new NumberSetting("count","Количество","Частиц на одно событие",16,4,32,1));
    private final NumberSetting size=setting(new NumberSetting("size","Размер","Размер частиц",.16,.08,.3,.02));
    private final ChoiceSetting shape=setting(new ChoiceSetting("shape","Форма","Текстуры частиц","Все",SHAPES));
    private final ChoiceSetting motion=setting(new ChoiceSetting("motion","Движение","Направление разлёта","Закручивание",List.of("Разлёт","Закручивание")));
    private final NumberSetting lifetime=setting(new NumberSetting("lifetime","Длительность, мс","Время до исчезновения",1600,350,2500,50));
    private final NumberSetting speed=setting(new NumberSetting("speed","Скорость","Скорость разлёта",.7,.1,2,.1));
    private final NumberSetting gravity=setting(new NumberSetting("gravity","Гравитация","Положительное значение тянет частицы вниз",.2,-.5,1,.05));
    private final BooleanSetting shrink=setting(new BooleanSetting("shrink","Уменьшение","Плавно уменьшать частицы при исчезновении",true));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Использовать цвет темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0xFF6688));
    private final HitParticles particles=new HitParticles();
    private final Random random=new Random();
    private Object world;
    private final BooleanSetting hits=setting(new BooleanSetting("hits","Удары","Частицы при получении урона",true));
    private final BooleanSetting jumps=setting(new BooleanSetting("jumps","Прыжок","Частицы при прыжке",true));
    private final BooleanSetting landings=setting(new BooleanSetting("landings","Приземление","Частицы при приземлении",true));
    private final BooleanSetting deaths=setting(new BooleanSetting("deaths","Смерть цели","Частицы при смерти цели",true));
    private final BooleanSetting totems=setting(new BooleanSetting("totems","Тотем","Частицы при использовании тотема",true));
    private LivingEntity tracked;
    private boolean ground, movementReady;
    private WorldSprite sprites;
    public enum Event { HIT, JUMP, LANDING, DEATH, TOTEM }
    public Particles(){super("particles","Particles","Частицы ударов, прыжков, приземлений, смерти цели и тотема",ModuleCategory.VISUALS);color.visibleWhen(()->!theme.value());}
    @Override protected void onDisable(){particles.clear();world=null;tracked=null;movementReady=false;sprites=null;}
    public static void emit(net.minecraft.world.entity.Entity entity,Event event){
        var services=dev.sqwat.vela.VelaClient.services();
        if(services!=null&&services.modules().byId("particles") instanceof Particles effect)effect.accept(entity,event);
    }
    private void accept(net.minecraft.world.entity.Entity entity,Event event){
        var mc=Minecraft.getInstance();
        if(!enabled()||mc.level==null||mc.player==null||entity==null||entity.level()!=mc.level||entity.isInvisible()||entity.distanceToSqr(mc.player)>4096)return;
        boolean selected=switch(event){case HIT->hits.value();case JUMP->jumps.value();case LANDING->landings.value();case DEATH->deaths.value();case TOTEM->totems.value();};
        if(!selected)return;
        if(world!=mc.level){onDisable();world=mc.level;}
        double height=event==Event.JUMP||event==Event.LANDING?.15:entity.getBbHeight()*.55;
        particles.burst(entity.position().add(0,height,0),count.value().intValue(),System.nanoTime(),random,lifetime.value().longValue()*1_000_000L,speed.value(),gravity.value(),motion.value().equals("Закручивание")?1.8:0);
    }
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();var services=dev.sqwat.vela.VelaClient.services();
        if(mc.level==null||mc.player==null||services==null){onDisable();return;}
        if(world!=mc.level){onDisable();world=mc.level;}
        long now=System.nanoTime();particles.expire(now);
        boolean onGround=mc.player.onGround();
        if(movementReady){
            if(ground&&!onGround&&mc.player.getDeltaMovement().y>0)accept(mc.player,Event.JUMP);
            if(!ground&&onGround)accept(mc.player,Event.LANDING);
        }
        ground=onGround;movementReady=true;
        if(tracked!=null&&tracked.level()==mc.level&&tracked.isDeadOrDying()){accept(tracked,Event.DEATH);tracked=null;}
        var target=services.hudLayout().visualTarget(mc);
        if(target!=null)tracked=target;
        else if(tracked!=null&&tracked.isRemoved())tracked=null;
    }
    public void draw(GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();
        if(!enabled()||world!=mc.level||particles.size()==0||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();int rgb=theme.value()?VelaTheme.PRIMARY:color.value();
        if(sprites==null)sprites=new WorldSprite(true);else sprites.begin();
        int chosen=SHAPES.indexOf(shape.value())-1;float baseSize=size.value().floatValue()*2;boolean shrinking=shrink.value();
        for(var p:particles.particles()){
            double alpha=p.opacity(now);if(alpha<=0)continue;
            int selected=chosen<0?Math.floorMod(Float.floatToIntBits(p.rotation()),ParticleAtlas.COUNT):chosen;
            double fade=Math.sqrt(alpha);float scale=shrinking?(float)(.4+.6*fade):1;
            sprites.drawRegion(g,ATLAS,128,128,p.position(now),baseSize*scale,p.rotation()+(float)((now-p.born())/1e9)*.8f,NeonGeometry.tint(rgb,fade),ParticleAtlas.u(selected),ParticleAtlas.v(selected),ParticleAtlas.u1(selected),ParticleAtlas.v1(selected));
        }
        sprites.flush(g);
    }
}
