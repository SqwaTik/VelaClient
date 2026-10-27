package dev.sqwat.vela.module.visual;

import com.mojang.authlib.GameProfile;
import dev.sqwat.vela.module.visual.DeathRecap.Actor;
import dev.sqwat.vela.module.visual.DeathRecap.Recording;
import dev.sqwat.vela.module.visual.DeathRecap.Snap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/**
 * Plays a {@link Recording} back in the current level: client-only ghost copies of the recorded entities are moved
 * through the frames while the real ones are hidden, and the camera is placed by {@link #camera}. Nothing is sent to
 * the server. Render thread only.
 */
public final class DeathReplay {
    public enum View{ORBIT,SELF,KILLER}
    private static Recording recording;
    private static final Map<UUID,Entity> ghosts=new HashMap<>();
    private static final Set<Entity> ghostSet=Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<WalkAnimationState> ghostWalks=Collections.newSetFromMap(new IdentityHashMap<>());
    private static ClientLevel level;
    private static float time;           // ticks since the first frame
    private static boolean playing=true;
    private static float speed=1f;
    /** Per actor: for every frame, the newest frame at or before it that contains the actor (-1 before it appears). */
    private static final Map<UUID,int[]> lastSeen=new HashMap<>();
    private static int endIndex;
    private static View view=View.ORBIT;
    private static float orbitYaw,orbitPitch=22,orbitDistance=5.5f;
    private static boolean autoOrbit=true;
    private static long lastFrame;
    private static Vec3 cameraPos;private static float cameraYaw,cameraPitch;
    private DeathReplay(){}

    public static void open(Recording r){Minecraft.getInstance().gui.setScreen(new dev.sqwat.vela.ui.screen.DeathRecapScreen(r,Minecraft.getInstance().gui.screen()));}
    public static boolean active(){return recording!=null;}
    public static boolean isGhost(Entity e){return !ghostSet.isEmpty()&&ghostSet.contains(e);}
    /** Limb state of a ghost: its phase is set per frame, so the renderer must not extrapolate it. */
    public static boolean isGhostWalk(WalkAnimationState state){return !ghostWalks.isEmpty()&&ghostWalks.contains(state);}
    /** Real entities that are being replayed (and the local player) are hidden while their ghosts play. */
    public static boolean hides(Entity e){
        if(recording==null||ghostSet.contains(e))return false;
        if(e==Minecraft.getInstance().player)return true;
        return recording.actors().containsKey(e.getUUID());
    }

    private static boolean hud;
    /** A saved replay may keep the game HUD visible; the live recap never shows it. */
    public static boolean showsHud(){return hud;}
    public static void showHud(boolean value){hud=value;}
    public static void start(Recording r){start(r,View.ORBIT,false,1f);}
    public static void start(Recording r,View initial,boolean showHud,float playbackSpeed){
        stop();
        var mc=Minecraft.getInstance();if(mc.level==null||r.frames().isEmpty())return;
        recording=r;level=mc.level;time=0;playing=true;autoOrbit=true;lastFrame=0;hud=showHud;speed=playbackSpeed;
        // The replay ends one second after the death; nothing after that is interesting.
        endIndex=r.frames().size()-1;
        for(int i=0;i<r.frames().size();i++)if(r.frames().get(i).tick()>r.deathTick()+20){endIndex=Math.max(1,i-1);break;}
        lastSeen.clear();
        for(var actor:r.actors().keySet()){
            int[] seen=new int[r.frames().size()];int last=-1;
            for(int i=0;i<seen.length;i++){if(r.frames().get(i).snaps().containsKey(actor))last=i;seen[i]=last;}
            lastSeen.put(actor,seen);
        }
        var first=r.frames().getFirst().snaps().get(r.self());
        orbitYaw=first==null?0:first.yRot()+150;orbitPitch=22;orbitDistance=5.5f;view=initial==View.KILLER&&r.killer()==null?View.ORBIT:initial;
        int id=Integer.MAX_VALUE-1024;
        for(var actor:r.actors().values()){
            Entity ghost=create(actor);if(ghost==null)continue;
            while(level.getEntity(id)!=null)id--;
            ghost.setId(id--);ghost.setNoGravity(true);
            ghosts.put(actor.uuid(),ghost);ghostSet.add(ghost);
            if(ghost instanceof LivingEntity living)ghostWalks.add(living.walkAnimation);
            apply(ghost,actor,0);
            level.addEntity(ghost);
        }
    }
    private static Entity create(Actor actor){
        if(actor.player())return new Ghost(level,new GameProfile(UUID.randomUUID(),actor.name()),skin(actor));
        try{return actor.type().create(level,EntitySpawnReason.LOAD);}catch(RuntimeException unsupported){return null;}
    }
    /** Recorded skin, or for a saved replay the player's current skin if they are online, else their default one. */
    private static PlayerSkin skin(Actor actor){
        if(actor.skin()!=null)return actor.skin();
        var connection=Minecraft.getInstance().getConnection();
        var info=connection==null?null:connection.getPlayerInfo(actor.uuid());
        if(info==null&&connection!=null)info=connection.getPlayerInfo(actor.name());
        return info!=null?info.getSkin():net.minecraft.client.resources.DefaultPlayerSkin.get(actor.uuid());
    }
    public static void stop(){
        if(level!=null)for(var ghost:ghosts.values())level.removeEntity(ghost.getId(),Entity.RemovalReason.DISCARDED);
        ghosts.clear();ghostSet.clear();ghostWalks.clear();lastSeen.clear();recording=null;hud=false;level=null;cameraPos=null;
    }

    public static float length(){return recording==null?0:recording.frames().get(endIndex).tick()-recording.firstTick();}
    public static float time(){return time;}
    public static void seek(float ticks){time=Math.clamp(ticks,0,length());}
    public static boolean playing(){return playing;}
    public static void toggle(){if(time>=length())time=0;playing=!playing;}
    public static float speed(){return speed;}
    public static void speed(float value){speed=value;}
    public static View view(){return view;}
    public static void view(View v){view=v;}
    public static boolean hasKiller(){return recording!=null&&recording.killer()!=null&&ghosts.containsKey(recording.killer());}
    public static void orbit(float dyaw,float dpitch){orbitYaw+=dyaw;orbitPitch=Math.clamp(orbitPitch+dpitch,-80,85);autoOrbit=false;}
    public static void zoom(float amount){orbitDistance=Math.clamp(orbitDistance*(float)Math.pow(.88,amount),1.5f,24);}

    /** Advances playback by real time and moves every ghost; called once per rendered frame. */
    public static void frame(){
        if(recording==null)return;
        if(Minecraft.getInstance().level!=level){stop();return;}
        long now=System.nanoTime();float dt=lastFrame==0?0:Math.min(.1f,(now-lastFrame)/1e9f);lastFrame=now;
        if(playing){time+=dt*20*speed;if(time>=length()){time=length();playing=false;}}
        if(autoOrbit)orbitYaw+=dt*9;
        for(var e:ghosts.entrySet())apply(e.getValue(),recording.actors().get(e.getKey()),time);
        place();
    }
    /**
     * State at a frame; an entity that is no longer in the recording (left the radius, or the recording stopped seeing
     * it) holds its last recorded state instead of vanishing.
     */
    private static Snap at(UUID uuid,int index){
        var frames=recording.frames();index=Math.clamp(index,0,frames.size()-1);
        var direct=frames.get(index).snaps().get(uuid);
        if(direct!=null)return direct;
        var seen=lastSeen.get(uuid);
        return seen==null||seen[index]<0?null:frames.get(seen[index]).snaps().get(uuid);
    }
    /** Interpolated state; ghosts absent from both neighbouring frames are made invisible. */
    private static void apply(Entity ghost,Actor actor,float t){
        int i=(int)Math.floor(t);float f=t-i;
        Snap a=at(actor.uuid(),i),b=at(actor.uuid(),i+1);
        if(a==null)a=b;if(b==null)b=a;
        boolean hidden=a==null||actor.self()&&view==View.SELF||actor.uuid().equals(recording.killer())&&view==View.KILLER;
        ghost.setInvisible(hidden);
        if(a==null)return;
        double x=Mth.lerp(f,a.x(),b.x()),y=Mth.lerp(f,a.y(),b.y()),z=Mth.lerp(f,a.z(),b.z());
        float yaw=Mth.rotLerp(f,a.yRot(),b.yRot()),pitch=Mth.lerp(f,a.xRot(),b.xRot());
        ghost.setPos(x,y,z);ghost.xo=ghost.xOld=x;ghost.yo=ghost.yOld=y;ghost.zo=ghost.zOld=z;
        ghost.setYRot(yaw);ghost.yRotO=yaw;ghost.setXRot(pitch);ghost.xRotO=pitch;
        ghost.setPose(f<.5f?a.pose():b.pose());
        if(ghost instanceof LivingEntity living){
            float head=Mth.rotLerp(f,a.headYaw(),b.headYaw()),body=Mth.rotLerp(f,a.bodyYaw(),b.bodyYaw());
            living.yHeadRot=living.yHeadRotO=head;living.yBodyRot=living.yBodyRotO=body;
            var walk=(dev.sqwat.vela.mixin.entity.WalkAnimationAccessor)living.walkAnimation;
            float position=Mth.lerp(f,a.walkPosition(),b.walkPosition()),walkSpeed=Mth.lerp(f,a.walkSpeed(),b.walkSpeed());
            walk.vela$position(position);walk.vela$speed(walkSpeed);walk.vela$speedOld(walkSpeed);
            living.attackAnim=living.oAttackAnim=Mth.lerp(f,a.attackAnim(),b.attackAnim());
            living.swinging=a.swinging();living.swingingArm=a.arm();living.swingTime=a.swingTime();
            living.hurtTime=a.hurtTime();living.deathTime=a.deathTime();
            var slots=new EquipmentSlot[]{EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
            for(int s=0;s<slots.length;s++)if(living.getItemBySlot(slots[s])!=a.gear()[s])living.setItemSlot(slots[s],a.gear()[s]);
        }
    }
    /** Computes the camera for the current view; the orbit is clipped against blocks. */
    private static void place(){
        var self=at(recording.self(),(int)Math.floor(time));
        if(view==View.SELF&&self!=null){eye(recording.self(),self);return;}
        if(view==View.KILLER&&recording.killer()!=null){var k=at(recording.killer(),(int)Math.floor(time));if(k!=null){eye(recording.killer(),k);return;}}
        Vec3 center=self==null?cameraPos:new Vec3(ghosts.get(recording.self()).getX(),ghosts.get(recording.self()).getY()+1.1,ghosts.get(recording.self()).getZ());
        if(center==null)return;
        double yaw=Math.toRadians(orbitYaw),pitch=Math.toRadians(orbitPitch);
        var dir=new Vec3(-Math.sin(yaw)*Math.cos(pitch),Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch));
        var wanted=center.add(dir.scale(orbitDistance));
        var hit=level.clip(new ClipContext(center,wanted,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,CollisionContext()));
        cameraPos=hit.getType()==HitResult.Type.MISS?wanted:hit.getLocation().add(dir.scale(-.25));
        // Look back at the centre.
        cameraYaw=(float)Math.toDegrees(Math.atan2(dir.x,-dir.z));cameraPitch=(float)Math.toDegrees(Math.asin(dir.y));
    }
    private static net.minecraft.world.phys.shapes.CollisionContext CollisionContext(){return net.minecraft.world.phys.shapes.CollisionContext.empty();}
    private static void eye(UUID uuid,Snap s){
        var ghost=ghosts.get(uuid);var actor=recording.actors().get(uuid);
        double eye=s.pose()==Pose.CROUCHING?actor.eyeHeight()*.82:actor.eyeHeight();
        cameraPos=new Vec3(ghost.getX(),ghost.getY()+eye,ghost.getZ());
        cameraYaw=ghost instanceof LivingEntity l?l.yHeadRot:ghost.getYRot();cameraPitch=ghost.getXRot();
    }
    /** Camera override for mixin.camera.DeathRecapCameraMixin; null when no replay runs. */
    public static Vec3 cameraPosition(){return recording==null?null:cameraPos;}
    public static float cameraYaw(){return cameraYaw;}
    public static float cameraPitch(){return cameraPitch;}

    /** A player stand-in that never moves on its own and wears the recorded skin. */
    private static final class Ghost extends AbstractClientPlayer {
        private final PlayerSkin skin;
        Ghost(ClientLevel level,GameProfile profile,PlayerSkin skin){super(level,profile);this.skin=skin;}
        @Override public PlayerSkin getSkin(){return skin!=null?skin:super.getSkin();}
        @Override public void tick(){}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean isCreative(){return false;}
        @Override public boolean shouldShowName(){return true;}
    }
}
