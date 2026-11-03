package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.*;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;

/**
 * Bounded world-space predictions with a separately rendered impact label. The throw preview is drawn from the hand
 * (not from the eyes, where the whole arc would lie along the line of sight and show as one straight stroke), as a
 * smooth glowing curve that ends in a ring on the surface it lands on.
 */
public final class Trajectories extends ClientModule {
    private final BooleanSetting pearl=setting(new BooleanSetting("pearl","Эндер-жемчуг","Предсказывать бросок жемчуга",true));
    private final BooleanSetting snowball=setting(new BooleanSetting("snowball","Снежок","Предсказывать бросок снежка",true));
    private final BooleanSetting egg=setting(new BooleanSetting("egg","Яйцо","Предсказывать бросок яйца",true));
    private final BooleanSetting potion=setting(new BooleanSetting("potion","Зелье","Взрывные и туманные зелья",true));
    private final BooleanSetting experience=setting(new BooleanSetting("experience","Опыт","Пузырёк опыта",true));
    private final BooleanSetting flight=setting(new BooleanSetting("flight","После броска","Прогноз оставшегося пути летящих предметов",true));
    private final BooleanSetting time=setting(new BooleanSetting("time","Время попадания","Приблизительное время до столкновения",true));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Использовать цвет темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0x8877FF));
    private record Path(List<Vec3> points,Vec3 velocity,int entityId,String label,boolean hit,boolean airborne,Vec3 normal){}
    private volatile List<Path> paths=List.of();
    private Path preview;
    private long previewTime;
    private Item previewItem;
    private volatile Object world;
    private record CollisionSnapshot(Entity source,Vec3 origin,long tick,List<AABB> boxes){}
    private final Map<Integer,CollisionSnapshot> collisionSnapshots=new HashMap<>();
    public Trajectories(){
        super("trajectories","Trajectories","Прогноз броска и места попадания предметов",ModuleCategory.UTILITIES);
        color.visibleWhen(()->!theme.value());
    }
    @Override protected void onDisable(){paths=List.of();preview=null;previewItem=null;previewTime=0;world=null;collisionSnapshots.clear();}
    private boolean supports(ItemStack item){
        return item.is(Items.ENDER_PEARL)&&pearl.value()||item.is(Items.SNOWBALL)&&snowball.value()
            ||(item.is(Items.EGG)||item.is(Items.BLUE_EGG)||item.is(Items.BROWN_EGG))&&egg.value()
            ||(item.is(Items.SPLASH_POTION)||item.is(Items.LINGERING_POTION))&&potion.value()
            ||item.is(Items.EXPERIENCE_BOTTLE)&&experience.value();
    }
    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){onDisable();return;}
        if(world!=mc.level)onDisable();world=mc.level;
        collisionSnapshots.clear();
        var next=new ArrayList<Path>(9);
        ItemStack held=mc.player.getMainHandItem();
        if(!supports(held))held=mc.player.getOffhandItem();
        if(!supports(held))preview=null;
        if(flight.value())for(var entity:mc.level.entitiesForRendering()){
            if(next.size()>=9)break;
            if(entity instanceof ThrowableItemProjectile projectile&&supports(projectile.getItem())
                &&(projectile.getOwner()==mc.player||entity.distanceToSqr(mc.player)<65536)&&!entity.isRemoved())
                next.add(predict(mc,projectile,projectile.position(),projectile.getDeltaMovement(),projectile.getGravity()));
        }
        paths=List.copyOf(next);
    }
    private Path predict(Minecraft mc,Entity source,Vec3 start,Vec3 velocity,double gravity){
        var points=new ArrayList<Vec3>(241);points.add(start);
        var terrain=new dev.sqwat.vela.render.effect.TrajectoryTerrain(mc.level);
        Vec3 initialVelocity=velocity;
        var candidates=collisionBoxes(mc,source,start);
        Vec3 at=start,normal=null;boolean hit=false,known=true;double ticks=0;
        for(int i=0;i<240;i++){
            var block=BlockPos.containing(at);
            known&=terrain.known(block);
            boolean water=terrain.getFluidState(block).is(FluidTags.WATER);
            velocity=dev.sqwat.vela.render.effect.ThrowPhysics.velocity(velocity,gravity,water);
            Vec3 end=at.add(velocity);
            known&=terrain.known(BlockPos.containing(end));
            var impact=terrain.clip(new ClipContext(at,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,source));
            Vec3 nearest=impact.getType()==HitResult.Type.MISS?end:impact.getLocation();
            hit=impact.getType()!=HitResult.Type.MISS;
            if(hit)normal=impact.getDirection().getUnitVec3();
            Vec3 entityImpact=dev.sqwat.vela.render.effect.ProjectileCollisions.nearest(at,nearest,candidates);
            // An entity is hit facing the throw.
            if(entityImpact!=nearest){nearest=entityImpact;hit=true;normal=velocity.lengthSqr()<1e-10?new Vec3(0,1,0):velocity.normalize().scale(-1);}
            double fraction=velocity.lengthSqr()<1e-10?1:Math.min(1,at.distanceTo(nearest)/velocity.length());
            ticks=i+fraction;
            points.add(nearest);at=nearest;
            if(hit||at.y<mc.level.getMinY())break;
        }
        boolean airborne=source instanceof ThrowableItemProjectile;
        return new Path(List.copyOf(points),initialVelocity,source.getId(),
            hit&&known?String.format(Locale.ROOT,dev.sqwat.vela.ui.Lang.tr("%.1f с"),ticks/20):"В полёте",hit,airborne,normal);
    }
    private List<AABB> collisionBoxes(Minecraft mc,Entity source,Vec3 start){
        long tick=mc.level.getGameTime();
        var cached=collisionSnapshots.get(source.getId());
        if(cached!=null&&cached.source==source&&cached.tick==tick&&cached.origin.distanceToSqr(start)<=1)return cached.boxes;
        Entity owner=source instanceof ThrowableItemProjectile projectile?projectile.getOwner():mc.player;
        // One-block margin covers interpolation until the next tick; teleports invalidate immediately.
        var entities=mc.level.getEntities(source,new AABB(start,start).inflate(129),
            entity->entity.isPickable()&&!entity.isSpectator()&&entity!=owner);
        var boxes=new ArrayList<AABB>(Math.min(64,entities.size()));
        for(int i=0;i<Math.min(64,entities.size());i++)boxes.add(entities.get(i).getBoundingBox().inflate(.15));
        var snapshot=new CollisionSnapshot(source,start,tick,List.copyOf(boxes));
        if(collisionSnapshots.size()>=10)collisionSnapshots.clear();
        collisionSnapshots.put(source.getId(),snapshot);
        return snapshot.boxes;
    }
    private static Vec3 launch(Minecraft mc,float yawDegrees,float pitchDegrees,ItemStack item){
        boolean bottle=item.is(Items.EXPERIENCE_BOTTLE),pot=item.is(Items.SPLASH_POTION)||item.is(Items.LINGERING_POTION);
        double speed=bottle?.7:pot?.5:1.5;
        return dev.sqwat.vela.render.effect.ThrowPhysics.launch(yawDegrees,pitchDegrees,pot||bottle?-20:0,speed,mc.player.getKnownMovement(),mc.player.onGround());
    }
    public void submit(){
        var mc=Minecraft.getInstance();
        if(!enabled()||mc.player==null||world!=mc.level||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        var held=mc.player.getMainHandItem();if(!supports(held))held=mc.player.getOffhandItem();
        if(supports(held)){
            long now=System.nanoTime();
            Vec3 start=mc.player.getEyePosition(partial).add(0,-.1,0);
            Vec3 velocity=launch(mc,mc.player.getYRot(),mc.player.getXRot(),held);
            boolean changed=preview==null||preview.points.getFirst().distanceToSqr(start)>1e-10||preview.velocity.distanceToSqr(velocity)>1e-10;
            if(changed||previewItem!=held.getItem()||now-previewTime>=50_000_000L){
                boolean bottle=held.is(Items.EXPERIENCE_BOTTLE);
                boolean pot=held.is(Items.SPLASH_POTION)||held.is(Items.LINGERING_POTION);
                preview=predict(mc,mc.player,start,velocity,bottle?.07:pot?.05:.03);
                previewTime=now;
                previewItem=held.getItem();
            }
        }else preview=null;
        int tint=0xFF000000|((theme.value()?VelaTheme.PRIMARY:color.value())&0xFFFFFF);
        long ms=System.currentTimeMillis();
        if(preview!=null)submitPath(preview,smooth(preview.points,handOffset(mc,held,partial)),tint,ms);
        if(flight.value())for(var path:paths){
            var entity=mc.level.getEntity(path.entityId);
            if(entity==null||entity.isRemoved())continue;
            var points=new ArrayList<>(path.points);points.set(0,entity.getPosition(partial));
            submitPath(path,smooth(points,Vec3.ZERO),tint,ms);
        }
    }
    /**
     * Where the item leaves the hand, relative to the eyes: down and to the side of the arm holding it. In first person
     * the curve then rises from the hand at the bottom of the screen and bends into view; in third person the eyes
     * are not the camera, so no offset is needed.
     */
    private static Vec3 handOffset(Minecraft mc,ItemStack held,float partial){
        if(!mc.options.getCameraType().isFirstPerson())return Vec3.ZERO;
        var player=mc.player;
        boolean main=player.getMainHandItem()==held;
        boolean right=(player.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT)==main;
        Vec3 look=player.getViewVector(partial),up=new Vec3(0,1,0);
        Vec3 side=look.cross(up);
        if(side.lengthSqr()<1e-6)side=Vec3.directionFromRotation(0,player.getYRot(partial)-90);
        side=side.normalize();
        Vec3 down=side.cross(look).normalize();
        return side.scale(right?.32:-.32).add(down.scale(.22)).add(look.scale(.25));
    }
    /**
     * The tick points as a smooth curve (Catmull-Rom, a few steps per tick), with {@code offset} added at the start and
     * fading out over the first blocks, so the drawn line leaves the hand and joins the real path.
     */
    static List<Vec3> smooth(List<Vec3> points,Vec3 offset){
        int n=points.size();
        if(n<2)return points;
        var out=new ArrayList<Vec3>(n*3);
        double travelled=0,blend=5;
        for(int i=0;i<n-1;i++){
            Vec3 p0=points.get(Math.max(0,i-1)),p1=points.get(i),p2=points.get(i+1),p3=points.get(Math.min(n-1,i+2));
            double length=p1.distanceTo(p2);
            int steps=length<.25?1:length<1?2:4;
            for(int k=0;k<steps;k++){
                double t=k/(double)steps;
                Vec3 p=catmull(p0,p1,p2,p3,t);
                double f=Math.max(0,1-(travelled+length*t)/blend);
                out.add(p.add(offset.scale(f*f*(3-2*f))));
            }
            travelled+=length;
        }
        out.add(points.getLast());
        return out;
    }
    private static Vec3 catmull(Vec3 p0,Vec3 p1,Vec3 p2,Vec3 p3,double t){
        double t2=t*t,t3=t2*t;
        return p1.scale(2).add(p2.subtract(p0).scale(t)).add(p0.scale(2).subtract(p1.scale(5)).add(p2.scale(4)).subtract(p3).scale(t2))
            .add(p1.scale(3).subtract(p0).subtract(p2.scale(3)).add(p3).scale(t3)).scale(.5);
    }
    /** A soft wide glow under a crisp core, fading in from the hand, and a pulsing ring where it lands. */
    /**
     * Drawn against the camera without its walking bob, whether view bobbing is on or not: a line that starts at the
     * hand would otherwise swing across the screen with every step.
     */
    private static void submitPath(Path path,List<Vec3> curve,int tint,long ms){
        int rgb=tint&0xFFFFFF;
        var camera=Minecraft.getInstance().gameRenderer.mainCamera().position();
        var steady=new ArrayList<Vec3>(curve.size());
        for(var point:curve)steady.add(dev.sqwat.vela.render.view.ViewBob.steady(point,camera));
        curve=steady;
        double travelled=0;
        for(int i=1;i<curve.size();i++){
            Vec3 a=curve.get(i-1),b=curve.get(i);
            double step=a.distanceTo(b);
            if(step<1e-4)continue;
            travelled+=step;
            double alpha=Math.clamp(travelled/1.6,.1,1);
            net.minecraft.gizmos.Gizmos.line(a,b,(int)Math.round(60*alpha)<<24|rgb,9f).setAlwaysOnTop();
            net.minecraft.gizmos.Gizmos.line(a,b,(int)Math.round(255*alpha)<<24|lighter(rgb),3f).setAlwaysOnTop();
        }
        if(path.hit&&path.normal!=null)submitRing(path.points.getLast(),path.normal,rgb,ms,camera);
    }
    private static void submitRing(Vec3 center,Vec3 normal,int rgb,long ms,Vec3 camera){
        // Two unit vectors across the surface the ring lies on.
        Vec3 u=Math.abs(normal.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0).cross(normal).normalize();
        Vec3 v=normal.cross(u).normalize();
        Vec3 base=center.add(normal.scale(.02)),at=dev.sqwat.vela.render.view.ViewBob.steady(base,camera);
        float pulse=(float)(.5+.5*Math.sin(ms/1000.0*Math.PI*1.6));
        double radius=.32+.06*pulse;
        int segments=20;
        Vec3 previous=null;
        var fill=net.minecraft.gizmos.GizmoStyle.fill((int)(40+30*pulse)<<24|rgb);
        for(int i=0;i<=segments;i++){
            double angle=i*Math.PI*2/segments;
            Vec3 point=dev.sqwat.vela.render.view.ViewBob.steady(base.add(u.scale(Math.cos(angle)*radius)).add(v.scale(Math.sin(angle)*radius)),camera);
            if(previous!=null){
                net.minecraft.gizmos.Gizmos.line(previous,point,0xFF000000|lighter(rgb),3f).setAlwaysOnTop();
                net.minecraft.gizmos.Gizmos.rect(previous,at,at,point,fill).setAlwaysOnTop();
            }
            previous=point;
        }
        net.minecraft.gizmos.Gizmos.point(at,0xFF000000|lighter(rgb),6f).setAlwaysOnTop();
    }
    /** The tint a quarter of the way to white, so the core reads on dark ground and in the night sky. */
    private static int lighter(int rgb){
        int r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;
        return (r+(255-r)/4)<<16|(g+(255-g)/4)<<8|(b+(255-b)/4);
    }
    public void draw(net.minecraft.client.gui.GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();
        if(!enabled()||!flight.value()||!time.value()||mc.player==null||world!=mc.level
            ||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        int tint=0xFF000000|((theme.value()?VelaTheme.PRIMARY:color.value())&0xFFFFFF);
        var camera=mc.gameRenderer.mainCamera();var origin=camera.position();
        var matrix=camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f());
        float density=1.5f/Math.max(1,mc.getWindow().getGuiScale());
        var projected=new org.joml.Vector4f();
        g.pose().pushMatrix();g.pose().identity();
        try{for(var path:paths){
            var entity=mc.level.getEntity(path.entityId);
            if(entity==null||entity.isRemoved())continue;
            var point=path.points.getLast();
            projected.set((float)(point.x-origin.x),(float)(point.y-origin.y),(float)(point.z-origin.z),1).mul(matrix);
            if(projected.w<=.05f)continue;
            float x=(projected.x/projected.w+1)*g.guiWidth()/2f,y=(1-projected.y/projected.w)*g.guiHeight()/2f;
            if(x<0||y<0||x>=g.guiWidth()||y>=g.guiHeight())continue;
            g.pose().pushMatrix();
            try{
                g.pose().translate(x,y);g.pose().scale(density);
                var c=new dev.sqwat.vela.ui.render.GameUiCanvas(g);
                float width=c.textWidth(path.label,8,dev.sqwat.vela.ui.render.UiCanvas.Weight.MEDIUM)+30;
                c.round(-width/2,-29,width,21,6,VelaTheme.SURFACE);
                c.border(-width/2,-29,width,21,6,VelaTheme.BORDER);
                c.image("cooldowns",-width/2+6,-24,11,tint);
                c.text(path.label,-width/2+21,-22,8,VelaTheme.TEXT,dev.sqwat.vela.ui.render.UiCanvas.Weight.MEDIUM);
            }finally{g.pose().popMatrix();}
        }}finally{g.pose().popMatrix();}
    }
}
