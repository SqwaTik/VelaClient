package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.ChoiceSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.render.effect.NeonGeometry;
import dev.sqwat.vela.ui.render.DissolveBody;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Death presentation for the observed target; bounded to three short-lived effects. */
public final class KillEffect extends ClientModule {
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Режим","Луч с неба или растворение модели погибшего игрока или моба","Луч",List.of("Луч","Растворение")));
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность, с","Время луча и растворения",1.1,.3,5,.1));
    private record Effect(Vec3 center,long born,long lifetime,UUID player,DissolveBody body){}
    public long lifetimeNanos(){return Math.round(duration.value()*1_000_000_000L);}
    private final ArrayDeque<Effect> effects=new ArrayDeque<>();
    private LivingEntity tracked;private Object world;private long seen;
    private DissolveBody.PoseSnapshot remembered;
    private static final net.minecraft.resources.Identifier GLOW=net.minecraft.resources.Identifier.fromNamespaceAndPath("velaclient","textures/effects/glow.png");
    public KillEffect(){super("kill_effect","KillEffect","Эффект при гибели текущей цели",ModuleCategory.VISUALS);}
    @Override protected void onDisable(){tracked=null;remembered=null;world=null;effects.clear();seen=0;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();var services=dev.sqwat.vela.VelaClient.services();
        if(mc.player==null||mc.level==null||services==null){onDisable();return;}
        if(world!=mc.level){onDisable();world=mc.level;}
        long now=System.nanoTime();
        effects.removeIf(effect->now-effect.born>=effect.lifetime);
        if(tracked!=null){
            if(tracked.level()==mc.level&&tracked.isDeadOrDying()&&now-seen<3_000_000_000L){
                if(effects.size()>=3)effects.removeFirst();
                DissolveBody body=null;
                boolean dissolve=mode.value().equals("Растворение");
                if(dissolve&&remembered!=null){
                    try{body=DissolveBody.capture(tracked,remembered);}
                    catch(RuntimeException error){org.slf4j.LoggerFactory.getLogger("Vela").warn("Unable to capture death model",error);}
                }
                // The chosen mode only: a dissolve that could not capture the model shows nothing, never the beam.
                if(!dissolve||body!=null)effects.addLast(new Effect(tracked.position(),now,lifetimeNanos(),tracked.getUUID(),body));
                if(!dissolve||body!=null)mc.level.playLocalSound(tracked.getX(),tracked.getY(),tracked.getZ(),body==null?net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE:net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.PLAYERS,.55f,body==null?1.35f:.85f,false);
                tracked=null;remembered=null;
            }else if(tracked.isRemoved()||now-seen>=3_000_000_000L){tracked=null;remembered=null;}
        }
        var selected=services.hudLayout().visualTarget(mc);
        if(selected!=null&&selected.isAlive()){if(selected!=tracked)remembered=null;tracked=selected;seen=now;}
        if(mode.value().equals("Растворение")&&tracked!=null&&tracked.isAlive()){
            try{remembered=DissolveBody.remember(tracked);}catch(RuntimeException error){remembered=null;}
        }
    }
    public boolean replacesCorpse(LivingEntity entity){
        if(!enabled()||world!=entity.level()||!entity.isDeadOrDying())return false;
        for(var effect:effects)if(effect.body!=null&&effect.player.equals(entity.getUUID()))return true;
        return false;
    }
    public static double opacity(double age){return age<0||age>=1?0:Math.min(1,age/.08)*(1-age)*(1-age);}
    public void draw(net.minecraft.client.gui.GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();if(!enabled()||world!=mc.level||effects.isEmpty()||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();
        dev.sqwat.vela.render.effect.WorldSprite sprites=null;
        for(var effect:effects)if(effect.body!=null){
            double progress=(double)(now-effect.born)/effect.lifetime;
            effect.body.draw(g,progress,VelaTheme.PRIMARY);
            if(sprites==null)sprites=new dev.sqwat.vela.render.effect.WorldSprite(true);
            for(int i=0;i<32;i++){
                double age=progress*1.1-i*.012;if(age<0||age>.65)continue;
                double angle=i*2.3999632297;
                Vec3 pos=effect.body.particleOrigin(i).add(Math.cos(angle)*age*.8,age*.65+Math.sin(i*1.7)*age*.3,Math.sin(angle)*age*.8);
                sprites.draw(g,GLOW,180,180,pos,(float)(.095*(1-age/.65)+.025),0,NeonGeometry.tint(0xFF244C,Math.pow(1-age/.65,1.4)));
            }
        }
        if(sprites!=null)sprites.flush(g);
    }
    public void submit(){
        var mc=Minecraft.getInstance();if(!enabled()||world!=mc.level||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();int rgb=VelaTheme.PRIMARY;
        for(var effect:effects){
            if(effect.body!=null)continue;
            double age=(double)(now-effect.born)/effect.lifetime,alpha=opacity(age);if(alpha<=0)continue;
            double radius=.32+Math.sin(Math.min(1,age*3)*Math.PI)*.35;
            cylinder(effect.center,radius*2.4,rgb,alpha*.045);
            cylinder(effect.center,radius,rgb,alpha*.25);
            cylinder(effect.center,radius*.25,NeonGeometry.core(rgb),alpha*.9);
            NeonGeometry.ring(effect.center.add(0,.04,0),.35+age*2,.1,rgb,alpha);
        }
    }
    private static void cylinder(Vec3 center,double radius,int rgb,double opacity){
        var style=GizmoStyle.fill(NeonGeometry.tint(rgb,opacity));
        for(int i=0;i<NeonGeometry.SEGMENTS;i+=4){
            var a=NeonGeometry.point(center,radius,i);var b=NeonGeometry.point(center,radius,i+4);
            Gizmos.rect(a,b,b.add(0,64,0),a.add(0,64,0),style);
        }
    }
}
