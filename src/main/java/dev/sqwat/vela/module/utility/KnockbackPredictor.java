package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.ColorSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import dev.sqwat.vela.render.effect.NeonGeometry;
import dev.sqwat.vela.ui.render.VelaTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/**
 * Where your next hit would send the entity under your crosshair. It replays vanilla's knockback exactly (the 0.4
 * push away from the attacker, then the extra push along your look for a charged sprint hit and Knockback levels,
 * reduced by the target's knockback resistance) and simulates the flight with vanilla's gravity, drag, ground friction
 * and block collisions. Dangerous landings (lava, fire, cactus, the void, a fall over 3 blocks) turn the path red
 * with a label. A prediction for a target standing still; a moving player's own input changes the outcome.
 */
public final class KnockbackPredictor extends ClientModule {
    private final NumberSetting range=setting(new NumberSetting("range","Дальность","Показывать для целей ближе, блоков",6,3,10,.5));
    private final BooleanSetting playersOnly=setting(new BooleanSetting("players_only","Только игроки","Не показывать для мобов",false));
    private final BooleanSetting dangerLabel=setting(new BooleanSetting("danger_label","Подпись опасности","Падение, лава или пустота в точке приземления",true));
    private final BooleanSetting theme=setting(new BooleanSetting("client_color","Цвет клиента","Использовать цвет темы",true));
    private final ColorSetting color=setting(new ColorSetting("color","Цвет",0x8877FF));

    private static final int MAX_TICKS=50,SAFE_FALL=3;
    /** Path relative to the target's position at prediction time, so drawing can follow the interpolated target. */
    private record Prediction(int targetId,Vec3 origin,List<Vec3> points,String danger){}
    private volatile Prediction prediction;
    private Object world;

    public KnockbackPredictor(){
        super("knockback_predictor","Knockback Predictor","Куда отлетит цель от вашего удара: путь, приземление и опасность",ModuleCategory.UTILITIES);
        color.visibleWhen(()->!theme.value());
    }
    public static KnockbackPredictor current(){return dev.sqwat.vela.module.Modules.active(KnockbackPredictor.class);}
    @Override protected void onDisable(){prediction=null;world=null;}
    public boolean active(){return prediction!=null;}
    /** For diagnostics: the current prediction in words. */
    public String describe(){var p=prediction;return p==null?"none":"target "+p.targetId+", "+p.points.size()+" points, landing "+p.points.getLast()+", danger "+p.danger;}

    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.player.isSpectator()){prediction=null;return;}
        if(world!=mc.level){world=mc.level;prediction=null;}
        var target=target(mc.player,range.value());
        if(target==null||playersOnly.value()&&!(target instanceof Player)){prediction=null;return;}
        prediction=predict(mc.player,target);
    }
    /** The living entity under the crosshair within {@code reach}, line of sight respected. */
    private static LivingEntity target(Player player,double reach){
        Vec3 eye=player.getEyePosition(),look=player.getViewVector(1),end=eye.add(look.scale(reach));
        var blocks=player.level().clip(new net.minecraft.world.level.ClipContext(eye,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        if(blocks.getType()!=net.minecraft.world.phys.HitResult.Type.MISS)end=blocks.getLocation();
        var hit=ProjectileUtil.getEntityHitResult(player,eye,end,player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1),
            e->e instanceof LivingEntity living&&living.isAlive()&&!e.isSpectator()&&!e.isInvisible()&&e.isPickable(),eye.distanceToSqr(end));
        return hit!=null&&hit.getEntity() instanceof LivingEntity living?living:null;
    }

    private static Prediction predict(Player attacker,LivingEntity target){
        var level=target.level();
        double resistance=Math.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE),0,1);
        boolean charged=attacker.getAttackStrengthScale(.5f)>.9f;
        boolean sprintHit=attacker.isSprinting()&&charged;
        int enchant=LocalCombatRules.level(attacker.getWeaponItem(),net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK);
        float extra=(float)(attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK)+enchant)/2f+(sprintHit?.5f:0);
        boolean onGround=target.onGround();
        // A remote player's velocity on this client is not reliable; a standing target is the honest baseline.
        Vec3 v=target instanceof Player&&target!=attacker?Vec3.ZERO:target.getDeltaMovement();
        // 1. LivingEntity.hurtServer -> dealDefaultKnockback: 0.4 away from the attacker's position.
        v=knockback(v,.4*(1-resistance),attacker.getX()-target.getX(),attacker.getZ()-target.getZ(),onGround);
        // 2. Player.attack -> causeExtraKnockback along the attacker's yaw.
        if(extra>0){
            float yaw=attacker.getYRot()*Mth.DEG_TO_RAD;
            v=knockback(v,extra*(1-resistance),Mth.sin(yaw),-Mth.cos(yaw),onGround);
        }
        var points=new ArrayList<Vec3>(MAX_TICKS+1);
        AABB box=target.getBoundingBox();
        Vec3 origin=target.position(),pos=origin;
        points.add(Vec3.ZERO);
        double peak=pos.y;String danger=null;boolean grounded=onGround;
        double gravity=target.getGravity();
        for(int tick=0;tick<MAX_TICKS;tick++){
            // Collide like Entity.collide: Y first, then the larger horizontal axis.
            double dy=clip(level,target,box,0,v.y,0);box=box.move(0,dy,0);
            boolean xFirst=Math.abs(v.x)>=Math.abs(v.z);
            double dx,dz;
            if(xFirst){dx=clip(level,target,box,v.x,0,0);box=box.move(dx,0,0);dz=clip(level,target,box,0,0,v.z);box=box.move(0,0,dz);}
            else{dz=clip(level,target,box,0,0,v.z);box=box.move(0,0,dz);dx=clip(level,target,box,v.x,0,0);box=box.move(dx,0,0);}
            grounded=v.y<0&&dy!=v.y;
            pos=new Vec3((box.minX+box.maxX)/2,box.minY,(box.minZ+box.maxZ)/2);
            points.add(pos.subtract(origin));
            peak=Math.max(peak,pos.y);
            danger=hazard(level,pos,box);
            if(danger!=null)break;
            if(pos.y<level.getMinY()){danger=dev.sqwat.vela.ui.Lang.pick("Void","Пустота");break;}
            // LivingEntity.travelInAir: gravity, then drag (ground friction when standing).
            float friction=grounded?level.getBlockState(BlockPos.containing(pos.x,pos.y-.5,pos.z)).getBlock().getFriction()*.91f:.91f;
            v=new Vec3((dx!=v.x?0:v.x)*friction,(grounded?0:v.y-gravity)*.98,(dz!=v.z?0:v.z)*friction);
            if(grounded){
                double fall=peak-pos.y;
                if(fall>SAFE_FALL&&!level.getFluidState(BlockPos.containing(pos)).is(FluidTags.WATER)
                    &&!level.getBlockState(BlockPos.containing(pos.x,pos.y-.2,pos.z)).is(Blocks.HAY_BLOCK)
                    &&!level.getBlockState(BlockPos.containing(pos.x,pos.y-.2,pos.z)).is(Blocks.SLIME_BLOCK))
                    danger=String.format(java.util.Locale.ROOT,"%s %d · −%d HP",dev.sqwat.vela.ui.Lang.pick("Fall","Падение"),Math.round(fall),Math.max(1,(int)Math.ceil(fall-SAFE_FALL)));
                if(v.horizontalDistanceSqr()<1e-4)break;
                peak=pos.y;
            }
        }
        return new Prediction(target.getId(),origin,List.copyOf(points),danger);
    }
    /** LivingEntity.knockback with a given direction (towards the attacker; the push goes the other way). */
    static Vec3 knockback(Vec3 v,double power,double xd,double zd,boolean onGround){
        if(power<=0)return v;
        if(xd*xd+zd*zd<1e-5){xd=.01;zd=0;}
        Vec3 push=new Vec3(xd,0,zd).normalize().scale(power);
        return new Vec3(v.x/2-push.x,onGround?Math.min(.4,v.y/2+power):v.y,v.z/2-push.z);
    }
    /** The part of a move along one axis that fits, found by halving (fine enough at 1/64 block). */
    private static double clip(net.minecraft.world.level.Level level,LivingEntity entity,AABB box,double x,double y,double z){
        double amount=x+y+z;
        if(amount==0)return 0;
        if(level.noCollision(entity,box.move(x,y,z)))return amount;
        double lo=0,hi=1;
        for(int i=0;i<6;i++){double mid=(lo+hi)/2;if(level.noCollision(entity,box.move(x*mid,y*mid,z*mid)))lo=mid;else hi=mid;}
        return amount*lo;
    }
    private static String hazard(net.minecraft.world.level.Level level,Vec3 pos,AABB box){
        var feet=BlockPos.containing(pos.x,pos.y+.1,pos.z);
        if(level.getFluidState(feet).is(FluidTags.LAVA))return dev.sqwat.vela.ui.Lang.pick("Lava","Лава");
        var state=level.getBlockState(feet);
        if(state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)||state.is(Blocks.CAMPFIRE)&&state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT))return dev.sqwat.vela.ui.Lang.pick("Fire","Огонь");
        if(state.is(Blocks.CACTUS)||level.getBlockState(feet.below()).is(Blocks.MAGMA_BLOCK)&&pos.y-feet.getY()<.05)return dev.sqwat.vela.ui.Lang.pick("Burns","Обжигает");
        if(state.is(Blocks.POWDER_SNOW))return dev.sqwat.vela.ui.Lang.pick("Powder snow","Рыхлый снег");
        return null;
    }

    /** Path and landing ring, world gizmos (VisualGizmosMixin). */
    public void submit(float partial){
        var mc=Minecraft.getInstance();var p=prediction;
        if(p==null||world!=mc.level)return;
        var target=mc.level.getEntity(p.targetId);
        if(target==null)return;
        Vec3 base=target.getPosition(partial);
        int rgb=(p.danger!=null?0xFF4D5E:(theme.value()?VelaTheme.PRIMARY:color.value()))&0xFFFFFF;
        Vec3 previous=base.add(p.points.getFirst()).add(0,.1,0);
        int count=p.points.size();
        for(int i=1;i<count;i++){
            Vec3 point=base.add(p.points.get(i)).add(0,.1,0);
            // Dashed: every other segment, fading along the path.
            // On top: the path starts inside the target's body and would otherwise hide behind it.
            if(i%2==1)net.minecraft.gizmos.Gizmos.line(previous,point,NeonGeometry.tint(rgb,1-.5*i/count),3f).setAlwaysOnTop();
            previous=point;
        }
        Vec3 landing=base.add(p.points.getLast()).add(0,.03,0);
        NeonGeometry.ring(landing,.45,.08,rgb,p.danger!=null?1:.75);
    }

    /** Danger label at the landing point (HUD, every frame). */
    public void draw(net.minecraft.client.gui.GuiGraphicsExtractor g){
        var mc=Minecraft.getInstance();var p=prediction;
        if(p==null||p.danger==null||!dangerLabel.value()||world!=mc.level||!dev.sqwat.vela.ui.hud.HudVisibility.allowed(mc))return;
        var target=mc.level.getEntity(p.targetId);
        if(target==null)return;
        Vec3 point=target.getPosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)).add(p.points.getLast()).add(0,.6,0);
        var camera=mc.gameRenderer.mainCamera();var origin=camera.position();
        var projected=new org.joml.Vector4f((float)(point.x-origin.x),(float)(point.y-origin.y),(float)(point.z-origin.z),1).mul(camera.getViewRotationProjectionMatrix(new org.joml.Matrix4f()));
        if(projected.w<=.05f)return;
        float x=(projected.x/projected.w+1)*g.guiWidth()/2f,y=(1-projected.y/projected.w)*g.guiHeight()/2f;
        if(x<0||y<0||x>=g.guiWidth()||y>=g.guiHeight())return;
        float density=1.5f/Math.max(1,mc.getWindow().getGuiScale());
        g.pose().pushMatrix();
        try{
            g.pose().identity();g.pose().translate(x,y);g.pose().scale(density);
            var c=new dev.sqwat.vela.ui.render.GameUiCanvas(g);
            String text=p.danger;
            float width=c.textWidth(text,8,dev.sqwat.vela.ui.render.UiCanvas.Weight.MEDIUM)+16;
            c.round(-width/2,-12,width,17,6,0xE61A0D12);
            c.border(-width/2,-12,width,17,6,0xFFFF4D5E);
            c.text(text,-width/2+8,-8,8,0xFFFFE0E4,dev.sqwat.vela.ui.render.UiCanvas.Weight.MEDIUM);
        }finally{g.pose().popMatrix();}
    }
}
