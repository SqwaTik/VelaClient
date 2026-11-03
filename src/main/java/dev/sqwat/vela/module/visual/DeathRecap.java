package dev.sqwat.vela.module.visual;

import com.mojang.authlib.GameProfile;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.KeySetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/**
 * Death Recap: keeps the last seconds around the player (positions, poses, swings, equipment of nearby entities plus
 * the player's health, armour and ping) and, after a death, replays them from any angle with a timeline of what
 * happened (ui.screen.DeathRecapScreen, DeathReplay). Only what the client already received is recorded.
 * Client thread only.
 */
public final class DeathRecap extends ClientModule {
    private static final EquipmentSlot[] GEAR={EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final EquipmentSlot[] ARMOR={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    private static final int AFTER_DEATH=24,MAX_ENTITIES=16;

    public record Snap(UUID uuid,double x,double y,double z,float yRot,float xRot,float headYaw,float bodyYaw,Pose pose,
                       float walkPosition,float walkSpeed,float attackAnim,boolean swinging,InteractionHand arm,int swingTime,int hurtTime,int deathTime,ItemStack[] gear){}
    /** Who an entity in the recording is; recorded once. {@code skin} only for players. */
    public record Actor(UUID uuid,EntityType<?> type,String name,boolean self,boolean player,PlayerSkin skin,float eyeHeight){}
    public record Frame(int tick,float health,float absorption,float maxHealth,int armor,int ping,Map<UUID,Snap> snaps){}
    public enum Kind{HIT,TOTEM,CONSUME,EFFECT}
    public record Event(int tick,Kind kind,String title,String detail,float amount,UUID source){}
    public record Recording(List<Frame> frames,Map<UUID,Actor> actors,List<Event> events,UUID self,UUID killer,String killerName,Component message,int deathTick,long recordedAt,String world,String dimension){
        public int firstTick(){return frames.getFirst().tick();}
        public int lastTick(){return frames.getLast().tick();}
    }

    private final NumberSetting seconds=setting(new NumberSetting("seconds","Длительность","Сколько секунд до смерти сохранять",10,5,20,1));
    private final NumberSetting radius=setting(new NumberSetting("radius","Радиус","Записывать сущности ближе, блоков",32,8,64,4));
    private final BooleanSetting mobs=setting(new BooleanSetting("mobs","Мобы","Записывать не только игроков",true));
    private final KeySetting openKey=setting(new KeySetting("open_key","Последняя смерть",-1));
    private final KeySetting libraryKey=setting(new KeySetting("library_key","Все записи",-1));
    {setting(new dev.sqwat.vela.module.setting.ActionSetting("library","Открыть записи",dev.sqwat.vela.ui.screen.ReplayLibraryScreen::open));}
    private boolean libraryHeld;

    private final ArrayDeque<Frame> frames=new ArrayDeque<>();
    private final ArrayDeque<Event> events=new ArrayDeque<>();
    private final Map<UUID,Actor> actors=new HashMap<>();
    private final Map<UUID,ItemStack[]> lastGear=new HashMap<>();
    private record PendingHit(int tick,int causeId,String type){}
    private final ArrayDeque<PendingHit> hits=new ArrayDeque<>();
    private Set<String> lastEffects=Set.of();
    private float lastTotal=-1;
    private String usingItem;private int usingCount;
    private int tick,deathTick=-1;private Object player;private boolean keyHeld;
    private UUID lastAttacker;private String lastAttackerName;private Component deathMessage;
    private Recording recording;

    public DeathRecap(){super("death_recap","Death Recap","Повтор последних секунд перед смертью с таймлайном",ModuleCategory.UTILITIES);}
    public static DeathRecap current(){return dev.sqwat.vela.module.Modules.active(DeathRecap.class);}
    /** Adds "Повтор смерти" to the death screen; it fades in with Wasted's buttons when that effect is on. */
    public static void initialize(){
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client,screen,width,height)->{
            var recap=current();
            if(!(screen instanceof net.minecraft.client.gui.screens.DeathScreen)||recap==null)return;
            int y=Wasted.presenting()?Wasted.buttonTop(height)+52:height/4+124;
            // "Death replay" when there is one, and "Recordings" for everything saved; side by side like vanilla rows.
            var buttons=new java.util.ArrayList<net.minecraft.client.gui.components.Button>();
            boolean replay=recap.hasRecording();
            if(replay)buttons.add(net.minecraft.client.gui.components.Button.builder(Component.literal(dev.sqwat.vela.ui.Lang.tr("Повтор смерти")),b->{
                var r=recap.recording();if(r!=null)DeathReplay.open(r);
            }).bounds(width/2-100,y,98,20).build());
            buttons.add(net.minecraft.client.gui.components.Button.builder(Component.literal(dev.sqwat.vela.ui.Lang.tr("Записи")),b->dev.sqwat.vela.ui.screen.ReplayLibraryScreen.open())
                .bounds(replay?width/2+2:width/2-100,y,replay?98:200,20).build());
            var widgets=net.fabricmc.fabric.api.client.screen.v1.Screens.getWidgets(screen);
            widgets.addAll(buttons);
            net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.beforeExtract(screen).register((s,g,mx,my,partial)->{for(var button:buttons)button.setAlpha(Wasted.presenting()?Wasted.buttonAlpha():1);});
        });
    }
    @Override protected void onDisable(){reset();recording=null;DeathReplay.stop();}
    private void reset(){lastTotal=-1;frames.clear();events.clear();actors.clear();lastGear.clear();hits.clear();deathTick=-1;lastAttacker=null;lastAttackerName=null;deathMessage=null;}

    /** The newest finished recording, finishing a pending one early if the player asks for it right after dying. */
    public Recording recording(){if(deathTick>=0)finish();return recording;}
    public boolean hasRecording(){return recording!=null||deathTick>=0;}

    @Override public void onClientTick(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){if(deathTick>=0)finish();reset();player=null;return;}
        boolean down=dev.sqwat.vela.ui.input.BindInput.down(mc.getWindow().handle(),openKey.value());
        if(down&&!keyHeld&&mc.gui.screen()==null)openLatest(mc);
        keyHeld=down;
        boolean library=dev.sqwat.vela.ui.input.BindInput.down(mc.getWindow().handle(),libraryKey.value());
        if(library&&!libraryHeld&&mc.gui.screen()==null)dev.sqwat.vela.ui.screen.ReplayLibraryScreen.open();
        libraryHeld=library;
        if(DeathReplay.active())return; // the replay moves ghosts in this level; do not record them
        if(player!=mc.player){if(deathTick>=0)finish();reset();player=mc.player;}
        tick++;
        var self=mc.player;
        Map<UUID,Snap> snaps=new HashMap<>(MAX_ENTITIES*2);
        snaps.put(self.getUUID(),snap(self,true));
        double r=radius.value();boolean withMobs=mobs.value();
        // Section lookup around the player instead of walking every rendered entity each tick.
        for(var living:mc.level.getEntitiesOfClass(LivingEntity.class,self.getBoundingBox().inflate(r),e->e!=self&&!e.isRemoved()&&!DeathReplay.isGhost(e)
                &&(e instanceof AbstractClientPlayer||withMobs&&!(e instanceof net.minecraft.world.entity.decoration.ArmorStand))&&e.distanceToSqr(self)<=r*r)){
            if(snaps.size()>MAX_ENTITIES)break;
            snaps.put(living.getUUID(),snap(living,false));
        }
        float health=self.getHealth(),absorption=self.getAbsorptionAmount();
        var info=mc.getConnection()==null?null:mc.getConnection().getPlayerInfo(self.getUUID());
        frames.addLast(new Frame(tick,health,absorption,self.getMaxHealth(),armor(self),info==null?0:info.getLatency(),Collections.unmodifiableMap(snaps)));
        observe(self,health+absorption);
        int keep=seconds.value().intValue()*20+AFTER_DEATH;
        while(frames.size()>keep)frames.removeFirst();
        while(!events.isEmpty()&&events.getFirst().tick()<frames.getFirst().tick())events.removeFirst();
        boolean dead=self.isDeadOrDying();
        if(dead&&deathTick<0)deathTick=tick;
        if(deathTick>=0&&(tick-deathTick>=AFTER_DEATH||!dead))finish();
    }
    /** The last death, or else the newest replay saved in this world and dimension. */
    private void openLatest(Minecraft mc){
        if(recording!=null){DeathReplay.open(recording);return;}
        var saved=ReplayFiles.list(dev.sqwat.vela.marker.MarkerRuntime.world(mc),dev.sqwat.vela.marker.MarkerRuntime.dimension(mc));
        if(saved.isEmpty())return;
        try{mc.gui.setScreen(new dev.sqwat.vela.ui.screen.DeathRecapScreen(ReplayFiles.load(saved.getFirst()),null,saved.getFirst().options()));}
        catch(Exception error){org.slf4j.LoggerFactory.getLogger("Vela/Recap").warn("Cannot open replay",error);}
    }
    private Snap snap(LivingEntity e,boolean self){
        var uuid=e.getUUID();
        actors.computeIfAbsent(uuid,k->new Actor(uuid,e.getType(),e.getName().getString(),self,e instanceof AbstractClientPlayer,e instanceof AbstractClientPlayer p?p.getSkin():null,e.getEyeHeight()));
        // Equipment rarely changes: frames share one array until something differs.
        var old=lastGear.get(uuid);boolean same=old!=null;
        for(int i=0;same&&i<GEAR.length;i++)same=ItemStack.matches(old[i],e.getItemBySlot(GEAR[i]));
        ItemStack[] gear=old;
        if(!same){gear=new ItemStack[GEAR.length];for(int i=0;i<GEAR.length;i++)gear[i]=e.getItemBySlot(GEAR[i]).copy();lastGear.put(uuid,gear);}
        return new Snap(uuid,e.getX(),e.getY(),e.getZ(),e.getYRot(),e.getXRot(),e.getYHeadRot(),e.yBodyRot,e.getPose(),
            e.walkAnimation.position(),e.walkAnimation.speed(),e.attackAnim,e.swinging,e.swingingArm==null?InteractionHand.MAIN_HAND:e.swingingArm,e.swingTime,e.hurtTime,e.deathTime,gear);
    }
    private static int armor(LivingEntity e){
        int sum=0,n=0;
        for(var slot:ARMOR){var stack=e.getItemBySlot(slot);if(stack.isEmpty()||!stack.isDamageableItem())continue;sum+=Math.round(100f*(stack.getMaxDamage()-stack.getDamageValue())/stack.getMaxDamage());n++;}
        return n==0?-1:sum/n;
    }
    /** Turns health changes, effects and finished item use into timeline events. */
    private void observe(LivingEntity self,float total){
        boolean previous=lastTotal>=0;
        while(!hits.isEmpty()&&tick-hits.getFirst().tick()>3)hits.removeFirst();
        if(previous){
            float drop=lastTotal-total;
            if(drop>.05f){
                var hit=hits.isEmpty()?null:hits.getLast();
                var attacker=hit==null?null:Minecraft.getInstance().level.getEntity(hit.causeId());
                String title=attacker!=null?attacker.getName().getString():hit!=null?damageName(hit.type()):"Урон";
                String weapon=attacker==null?"":weaponAt(attacker);
                if(attacker!=null){lastAttacker=attacker.getUUID();lastAttackerName=title;}
                events.addLast(new Event(tick,Kind.HIT,title,weapon,drop,attacker==null?null:attacker.getUUID()));
                hits.clear();
            }
        }
        var effects=new HashSet<String>();
        for(var effect:self.getActiveEffects()){
            String name=effect.getEffect().value().getDisplayName().getString()+(effect.getAmplifier()>0?" "+roman(effect.getAmplifier()+1):"");
            effects.add(name);
            if(!lastEffects.contains(name)&&previous)events.addLast(new Event(tick,Kind.EFFECT,name,"",0,null));
        }
        lastEffects=effects;lastTotal=total;
        String using=self.isUsingItem()?self.getUseItem().getHoverName().getString():null;
        if(usingItem!=null&&using==null){
            int now=countOf(self,usingItem);
            if(now<usingCount)events.addLast(new Event(tick,Kind.CONSUME,usingItem,"",0,null));
        }
        if(using!=null&&!using.equals(usingItem))usingCount=countOf(self,using);
        usingItem=using;
    }
    /**
     * What the attacker held when they swung, read from the recording: the damage packet arrives a little later, and by
     * then they may already have switched to another item.
     */
    private String weaponAt(net.minecraft.world.entity.Entity attacker){
        var uuid=attacker.getUUID();int checked=0;
        for(var it=frames.descendingIterator();it.hasNext()&&checked<10;checked++){
            var snap=it.next().snaps().get(uuid);
            if(snap!=null&&(snap.swinging()||snap.attackAnim()>0)&&!snap.gear()[0].isEmpty())return snap.gear()[0].getHoverName().getString();
        }
        return attacker instanceof LivingEntity l&&!l.getMainHandItem().isEmpty()?l.getMainHandItem().getHoverName().getString():"";
    }
    private static int countOf(LivingEntity self,String name){
        if(!(self instanceof net.minecraft.world.entity.player.Player p))return 0;
        int n=0;var inv=p.getInventory();
        for(int i=0;i<inv.getContainerSize();i++){var s=inv.getItem(i);if(!s.isEmpty()&&s.getHoverName().getString().equals(name))n+=s.getCount();}
        return n;
    }
    private static String roman(int n){return switch(n){case 2->"II";case 3->"III";case 4->"IV";case 5->"V";default->String.valueOf(n);};}
    private static String damageName(String type){
        return switch(type){case "fall"->"Падение";case "lava"->"Лава";case "in_fire","on_fire"->"Огонь";case "drown"->"Утопление";
            case "explosion","player_explosion"->"Взрыв";case "magic","indirect_magic"->"Зелье";case "wither"->"Иссушение";case "out_of_world"->"Пустота";default->"Урон";};
    }
    private void finish(){
        if(deathTick<0||frames.isEmpty()){deathTick=-1;return;}
        var self=frames.getLast().snaps().keySet().stream().filter(u->actors.get(u)!=null&&actors.get(u).self()).findFirst().orElse(null);
        var mc=Minecraft.getInstance();
        String world=mc.level==null?"":dev.sqwat.vela.marker.MarkerRuntime.world(mc),dimension=mc.level==null?"":dev.sqwat.vela.marker.MarkerRuntime.dimension(mc);
        recording=new Recording(List.copyOf(frames),Map.copyOf(actors),List.copyOf(events),self,lastAttacker,lastAttackerName,deathMessage,deathTick,System.currentTimeMillis(),world,dimension);
        deathTick=-1;
    }

    // Packet hooks (mixin.entity.DeathRecapEventsMixin), main thread.
    public void damaged(int causeId,String type){hits.addLast(new PendingHit(tick,causeId,type));}
    public void totem(){events.addLast(new Event(tick,Kind.TOTEM,"Тотем бессмертия","",0,null));}
    public void killed(Component message){deathMessage=message;}
}
