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

/** Bounded world-space predictions with a separately rendered impact label. */
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
    private record Path(List<Vec3> points,Vec3 velocity,int entityId,String label,boolean hit,boolean airborne){}
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
        Vec3 at=start;boolean hit=false,known=true;double ticks=0;
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
            Vec3 entityImpact=dev.sqwat.vela.render.effect.ProjectileCollisions.nearest(at,nearest,candidates);
            if(entityImpact!=nearest){nearest=entityImpact;hit=true;}
            double fraction=velocity.lengthSqr()<1e-10?1:Math.min(1,at.distanceTo(nearest)/velocity.length());
            ticks=i+fraction;
            points.add(nearest);at=nearest;
            if(hit||at.y<mc.level.getMinY())break;
        }
        boolean airborne=source instanceof ThrowableItemProjectile;
        return new Path(List.copyOf(points),initialVelocity,source.getId(),
            hit&&known?String.format(Locale.ROOT,dev.sqwat.vela.ui.Lang.tr("%.1f с"),ticks/20):"В полёте",hit,airborne);
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
        if(preview!=null)submitPath(preview,preview.points.getFirst(),tint,2.5,4.5);
        if(flight.value())for(var path:paths){
            var entity=mc.level.getEntity(path.entityId);
            if(entity!=null&&!entity.isRemoved())submitPath(path,entity.getPosition(partial),tint);
        }
    }
    private static void submitPath(Path path,Vec3 start,int tint){submitPath(path,start,tint,0,0);}
    /**
     * {@code hidden} blocks from the start are not drawn and the next {@code fade} fade in. For the throw preview the
     * path starts at the eyes: drawn from there it would cover the crosshair and swing with the walking bob (the
     * camera moves, the line right in front of it moves the most). The throw itself is unchanged.
     */
    private static void submitPath(Path path,Vec3 start,int tint,double hidden,double fade){
        Vec3 previous=start;double travelled=0;
        for(int i=1;i<path.points.size();i++){
            Vec3 point=path.points.get(i);
            double step=previous.distanceTo(point),from=travelled;travelled+=step;
            if(step>1e-4&&travelled>hidden){
                // Clip the first visible segment to where the line begins.
                Vec3 a=from<hidden?previous.add(point.subtract(previous).scale((hidden-from)/step)):previous;
                double alpha=fade<=0?1:Math.clamp((Math.max(from,hidden)-hidden)/fade,.15,1);
                int color=((int)Math.round(((tint>>>24)&255)*alpha)<<24)|(tint&0xFFFFFF);
                net.minecraft.gizmos.Gizmos.line(a,point,color,3.5f).setAlwaysOnTop();
            }
            previous=point;
        }
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
