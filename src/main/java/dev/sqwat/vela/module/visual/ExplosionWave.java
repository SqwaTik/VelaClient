package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.render.effect.NeonGeometry;
import dev.sqwat.vela.render.effect.ShockFront;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayDeque;

/**
 * A blast front for confirmed explosions that runs along the ground and flows around walls (see {@link ShockFront}).
 * Several explosions merge into one outline instead of crossing rings. When the front passes the player a gust of
 * wind is heard; a close explosion also stuns the screen for a moment (a dusty flash with ringing ears).
 */
public final class ExplosionWave extends ClientModule {
    public static final int LIMIT=6;
    private static final int STUN_MS=2600;
    private static final class Wave{
        final Vec3 center;final long born,life;final double radius;final ShockFront front;boolean passed;
        Wave(Vec3 center,long born,long life,double radius,ShockFront front){this.center=center;this.born=born;this.life=life;this.radius=radius;this.front=front;}
        double progress(long now){return (double)(now-born)/life;}
        /** A blast front slows as it spreads: fast at first, easing out. */
        double reach(long now){double t=Math.clamp(progress(now),0,1);return radius*(1-Math.pow(1-t,2.4));}
    }
    private final ArrayDeque<Wave> waves=new ArrayDeque<>();
    private final NumberSetting radius=setting(new NumberSetting("radius","Радиус","Как далеко расходится волна, блоков",8,3,15,.5));
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","Время расширения волны в секундах",1.1,.4,3,.1));
    private final BooleanSetting wind=setting(new BooleanSetting("wind","Порыв ветра","Звук ветра, когда волна проходит рядом",true));
    private final BooleanSetting stun=setting(new BooleanSetting("stun","Оглушение","Вспышка и звон в ушах при взрыве рядом",true));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Окрашивать волну цветом темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0x8877FF));
    private long stunStart;private double stunStrength;
    private Object world;
    public ExplosionWave(){
        super("explosion_wave","Explosion Wave","Ударная волна от взрывов по земле, огибает препятствия",ModuleCategory.VISUALS);
        color.visibleWhen(()->!theme.value());
    }
    public static void exploded(Vec3 center){
        var mc=Minecraft.getInstance();var services=dev.sqwat.vela.VelaClient.services();
        if(services==null||mc.level==null||mc.player==null||center.distanceToSqr(mc.player.position())>64*64)return;
        if(!(services.modules().byId("explosion_wave") instanceof ExplosionWave module)||!module.enabled())return;
        if(module.world!=mc.level){module.onDisable();module.world=mc.level;}
        module.spawn(center,System.nanoTime());
        module.stun(mc,center);
    }
    public void spawn(Vec3 center,long now){
        if(!enabled()||!Double.isFinite(center.x)||!Double.isFinite(center.y)||!Double.isFinite(center.z))return;
        expire(now);while(waves.size()>=LIMIT)waves.removeFirst();
        double r=radius.value();
        var mc=Minecraft.getInstance();var level=mc==null?null:mc.level;
        ShockFront front;
        try{
            front=level==null?ShockFront.open(center,r):ShockFront.compute(center,r,(x,y,z)->{
                var pos=new BlockPos(x,y,z);
                return !level.getBlockState(pos).getCollisionShape(level,pos).isEmpty();
            });
        }catch(RuntimeException unloaded){front=ShockFront.open(center,r);}
        waves.addLast(new Wave(center,now,(long)(duration.value()*1e9),r,front));
    }
    private void stun(Minecraft mc,Vec3 center){
        if(!stun.value())return;
        double distance=Math.sqrt(center.distanceToSqr(mc.player.getEyePosition()));
        double strength=Math.clamp(1-(distance-2)/(radius.value()*1.1),0,1);
        if(strength<.08)return;
        long now=System.currentTimeMillis();
        // A second close blast keeps the stronger stun instead of stacking.
        double remaining=stunStrength*Math.max(0,1-(now-stunStart)/(double)STUN_MS);
        if(strength<remaining)return;
        stunStart=now;stunStrength=strength;
        play("explosion.ring",(float)(.25+.55*strength),1);
        play("explosion.rumble",(float)(.35+.5*strength),1);
    }
    private static void play(String id,float volume,float pitch){
        var event=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("velaclient",id));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event,pitch,volume));
    }
    public int count(){return waves.size();}
    public void expire(long now){waves.removeIf(wave->now-wave.born>=wave.life);}
    @Override protected void onDisable(){waves.clear();world=null;stunStrength=0;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();var level=mc.level;if(world!=level){onDisable();world=level;}
        long now=System.nanoTime();expire(now);
        if(mc.player==null||!wind.value())return;
        var at=mc.player.position();
        for(var wave:waves){
            if(wave.passed)continue;
            double path=wave.front.distanceAt(at.x,at.z);
            if(!Double.isFinite(path)||Math.abs(at.y-wave.center.y)>6){wave.passed=true;continue;}
            if(path>wave.radius){wave.passed=true;continue;}
            if(wave.reach(now)>=path-.6){
                wave.passed=true;
                double near=1-path/wave.radius,direct=Math.clamp(Math.hypot(at.x-wave.center.x,at.z-wave.center.z)/Math.max(path,.01),0,1);
                play("explosion.wind",(float)(.3+.6*near*direct),(float)(.92+.16*Math.random()));
            }
        }
    }
    /** World-space front, submitted with the other gizmos. Outlines of overlapping waves are merged. */
    public void submit(){
        var mc=Minecraft.getInstance();
        if(!enabled()||waves.isEmpty()||mc.level==null||world!=mc.level||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();int tint=theme.value()?VelaTheme.PRIMARY:color.value();
        for(var wave:waves){
            double progress=wave.progress(now);if(progress<0||progress>=1)continue;
            double reach=wave.reach(now);if(reach<.3)continue;
            double appear=Math.min(1,progress/.06),alpha=appear*(1-progress*progress*progress);
            double wall=.25+1.1*(1-progress);
            for(var segment:wave.front.contour(reach)){
                var mid=segment.a().add(segment.b()).scale(.5);
                if(insideOther(wave,mid,now))continue;
                double strength=(segment.strengthA()+segment.strengthB())*.5;
                double a=alpha*(.25+.75*strength);if(a<.02)continue;
                double h=wall*(.4+.6*strength);
                Vec3 a0=segment.a().add(0,.06,0),b0=segment.b().add(0,.06,0),a1=a0.add(0,h,0),b1=b0.add(0,h,0);
                net.minecraft.gizmos.Gizmos.rect(a0,b0,b1,a1,net.minecraft.gizmos.GizmoStyle.fill(NeonGeometry.tint(tint,a*.3)));
                net.minecraft.gizmos.Gizmos.line(a1,b1,NeonGeometry.tint(tint,a*.35),7);
                net.minecraft.gizmos.Gizmos.line(a1,b1,NeonGeometry.tint(0xFFFFFF,a*.85),2.5f);
                net.minecraft.gizmos.Gizmos.line(a0,b0,NeonGeometry.tint(tint,a*.6),3);
            }
        }
    }
    private boolean insideOther(Wave self,Vec3 point,long now){
        for(var other:waves){
            if(other==self||other.progress(now)>=1||other.progress(now)<0)continue;
            if(other.front.distanceAt(point.x,point.z)<other.reach(now)-.25)return true;
        }
        return false;
    }
    /** Screen stun after a close blast: a quick dusty flash and darker edges that clear over ~2.6 s. */
    public void draw(GuiGraphicsExtractor g){
        if(!enabled()||stunStrength<=0)return;
        double t=(System.currentTimeMillis()-stunStart)/(double)STUN_MS;
        if(t>=1){stunStrength=0;return;}
        int w=g.guiWidth(),h=g.guiHeight();
        double flash=stunStrength*Math.max(0,1-t*5)*.55;                 // first ~0.5 s
        double haze=stunStrength*Math.pow(1-t,1.6)*.38;                 // dusty veil that settles
        int dust=(int)(Math.clamp(flash+haze,0,1)*255);
        if(dust>0)g.fill(0,0,w,h,(dust<<24)|0xE8E2D6);
        int edge=(int)(Math.clamp(stunStrength*Math.pow(1-t,1.2)*.7,0,1)*255);
        if(edge>0){
            int band=Math.max(8,h/4),dark=edge<<24;
            g.fillGradient(0,0,w,band,dark,0);
            g.fillGradient(0,h-band,w,h,0,dark);
            for(int i=0;i<band;i+=4){
                int a=(int)(edge*(1-i/(double)band));
                g.fill(i,0,i+4,h,a<<24);g.fill(w-i-4,0,w-i,h,a<<24);
            }
        }
    }
}
