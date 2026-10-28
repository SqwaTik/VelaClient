package dev.sqwat.vela.module.visual;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.render.effect.NeonGeometry;
import dev.sqwat.vela.render.effect.TrailGeometry;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Tick-owned sprite trail; bounded across the local player and visible friends. */
public final class DashTrail extends ClientModule {
    private final ChoiceSetting mode=setting(new ChoiceSetting("mode","Режим","Вид следа за игроком","Частицы",List.of("Частицы","Лента","Спираль")));
    private final NumberSetting duration=setting(new NumberSetting("duration","Длительность","За сколько секунд исчезает след",.8,.2,2,.1));
    private final NumberSetting width=setting(new NumberSetting("width","Размер","Высота ленты, ширина спирали и размер частиц",2.5,1,5,.5));
    private final BooleanSetting sprint=setting(new BooleanSetting("sprint_only","Только при беге","Создавать след только во время спринта",false));
    private final BooleanSetting firstPerson=setting(new BooleanSetting("first_person","От первого лица","Показывать свой след в обычном виде камеры",true));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Использовать цвет темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0x8877FF));
    private record Particle(Vec3 position,Vec3 velocity,long born,int frame,boolean local){}
    private final List<Particle> particles=new ArrayList<>();
    private final Map<UUID,Vec3> previous=new HashMap<>();
    private final Map<UUID,dev.sqwat.vela.render.effect.TrailHistory> histories=new HashMap<>();
    private record Path(List<dev.sqwat.vela.render.effect.TrailHistory.Point> points,List<TrailGeometry.Segment> segments,boolean local,double height,double width,boolean spiral){}
    private List<Path> paths=List.of();
    private String activeMode="";
    private Object world;private int sequence;
    private static final Identifier[] FRAMES=new Identifier[21];
    static{for(int i=0;i<FRAMES.length;i++)FRAMES[i]=Identifier.fromNamespaceAndPath("velaclient","textures/effects/dashcubics/dashcubic"+(i+1)+".png");}
    public DashTrail(){super("dash_trail","DashTrail","Частицы, лента или спираль за движущимся игроком",ModuleCategory.VISUALS);color.visibleWhen(()->!theme.value());}
    @Override protected void onDisable(){particles.clear();previous.clear();histories.clear();paths=List.of();world=null;}
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();var services=dev.sqwat.vela.VelaClient.services();
        if(mc.player==null||mc.level==null){onDisable();return;}
        if(world!=mc.level){onDisable();world=mc.level;}
        if(!activeMode.equals(mode.value())){particles.clear();histories.clear();paths=List.of();previous.clear();activeMode=mode.value();}
        long now=System.nanoTime();double life=duration.value()*1e9;
        particles.removeIf(p->now-p.born>=life);
        Set<UUID> present=new HashSet<>();int count=0;var nextPaths=new ArrayList<Path>(8);
        for(var player:mc.level.players()){
            boolean local=player==mc.player;
            if(!local&&(services==null||!services.configs().current().friendDashTrail||!services.friends().contains(player.getName().getString())))continue;
            if(!player.isAlive()||player.isInvisible()||player.distanceToSqr(mc.player)>32*32||count++>=8)continue;
            present.add(player.getUUID());var old=previous.put(player.getUUID(),player.position());
            if(!mode.value().equals("Частицы")){
                var history=histories.computeIfAbsent(player.getUUID(),id->new dev.sqwat.vela.render.effect.TrailHistory());
                history.expire(now,(long)life);
                if(!sprint.value()||player.isSprinting())history.update(player.position(),now,(long)life);
                else history.clear();
                var points=history.snapshot();var segments=new ArrayList<TrailGeometry.Segment>(Math.max(0,points.size()-1));
                boolean spiral=mode.value().equals("Спираль");double size=width.value();
                for(int i=1;i<points.size();i++)segments.add(TrailGeometry.segment(points.get(i-1),points.get(i),player.getBbHeight(),size,spiral,1));
                nextPaths.add(new Path(points,List.copyOf(segments),local,player.getBbHeight(),size,spiral));continue;
            }
            if(old==null||sprint.value()&&!player.isSprinting())continue;
            double distance=old.distanceTo(player.position());if(distance<.05||distance>4)continue;
            int steps=Math.clamp((int)Math.ceil(distance/.12),1,8);
            for(int step=0;step<steps;step++){
                int seed=sequence++;
                double height=.15+Math.floorMod(seed*37,97)/97d*(player.getBbHeight()-.2);
                var pos=old.lerp(player.position(),(step+.5)/steps).add(Math.sin(seed*2.4)*.2,height,Math.cos(seed*2.4)*.2);
                var velocity=new Vec3(Math.sin(seed)*.12,.08,Math.cos(seed)*.12);
                particles.add(new Particle(pos,velocity,now,Math.floorMod(seed,FRAMES.length),local));
            }
        }
        previous.keySet().retainAll(present);
        histories.keySet().retainAll(present);paths=List.copyOf(nextPaths);
        if(particles.size()>192)particles.subList(0,particles.size()-192).clear();
    }
    public void submit(){
        var mc=Minecraft.getInstance();if(!enabled()||world!=mc.level||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        long now=System.nanoTime();double life=duration.value()*1e9;int rgb=theme.value()?VelaTheme.PRIMARY:color.value();
        boolean firstPersonView=mc.options.getCameraType().isFirstPerson();var camera=mc.gameRenderer.mainCamera().position();
        for(var path:paths){
            if(path.local&&firstPersonView&&!firstPerson.value())continue;
            var points=path.points;
            for(int i=1;i<points.size();i++){
                var a=points.get(i-1);var b=points.get(i);
                double head=i==points.size()-1?Math.clamp((now-b.born())/50_000_000d,0,1):1;
                double age=Math.clamp((now-a.born())/life,0,1);
                double fade=1-Math.pow(age,4);
                // Own trail in first person: the part around the camera would wrap the view, so it starts behind you.
                if(path.local&&firstPersonView)fade*=clearance(camera,a.position(),b.position());
                if(fade<.01)continue;
                var segment=head>=1?path.segments.get(i-1):TrailGeometry.segment(a,b,path.height,path.width,path.spiral,head);
                if(!path.spiral){
                    net.minecraft.gizmos.Gizmos.rect(segment.firstStart(),segment.secondStart(),segment.secondEnd(),segment.firstEnd(),net.minecraft.gizmos.GizmoStyle.fill(NeonGeometry.tint(rgb,fade*.88)));
                    trailLine(segment.secondStart(),segment.secondEnd(),rgb,fade);
                    trailLine(segment.firstStart(),segment.firstEnd(),rgb,fade*.65);
                }else{
                    trailLine(segment.firstStart(),segment.firstEnd(),rgb,fade);
                    trailLine(segment.secondStart(),segment.secondEnd(),rgb,fade);
                }
            }
        }
    }
    /** 0 within 1.6 blocks of the camera (horizontally), fading in to 1 by 3.2 blocks. */
    private static double clearance(Vec3 camera,Vec3 a,Vec3 b){
        double near=Math.min(Math.hypot(a.x-camera.x,a.z-camera.z),Math.hypot(b.x-camera.x,b.z-camera.z));
        double t=Math.clamp((near-1.6)/1.6,0,1);
        return t*t*(3-2*t);
    }
    private static void trailLine(Vec3 a,Vec3 b,int rgb,double alpha){
        net.minecraft.gizmos.Gizmos.line(a,b,NeonGeometry.tint(rgb,alpha*.3),8);
        net.minecraft.gizmos.Gizmos.line(a,b,NeonGeometry.tint(rgb,alpha),4);
    }
    public void draw(net.minecraft.client.gui.GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();if(!enabled()||world!=mc.level||particles.isEmpty()||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        var sprites=new dev.sqwat.vela.render.effect.WorldSprite(true);long now=System.nanoTime();double life=duration.value()*1e9;
        int rgb=theme.value()?VelaTheme.PRIMARY:color.value();
        for(var p:particles){
            boolean own=p.local&&mc.options.getCameraType().isFirstPerson();
            if(own&&!firstPerson.value())continue;
            double age=Math.clamp((now-p.born)/life,0,1),fade=Math.sin(Math.PI*age)*(1-age);
            if(own)fade*=clearance(mc.gameRenderer.mainCamera().position(),p.position,p.position);
            if(fade<.01)continue;
            int frame=p.frame+1,tw=frame<=3?48:frame<=16?96:144,th=frame>=13&&frame<=16?96:48;
            sprites.draw(g,FRAMES[p.frame],tw,th,p.position.add(p.velocity.scale(age)),(float)(width.value()*.24*(1-age*.4)),(float)(p.frame*.7+age),NeonGeometry.tint(rgb,fade));
        }
        sprites.flush(g);
    }
}
