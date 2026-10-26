package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.audio.SoundClips;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.ActionSetting;
import dev.sqwat.vela.module.setting.BooleanSetting;
import dev.sqwat.vela.module.setting.DynamicChoiceSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plays a sound when the player attacks a living entity: one of Vela's built-in sounds (synthesized, shipped in the
 * mod, played through Minecraft's sound engine) or a .wav/.ogg file from config/velaclient/hitsounds. Optionally
 * mutes vanilla's attack sounds for the player's own hits.
 */
public final class HitSound extends ClientModule {
    /** Display name → sound id under assets/velaclient/sounds.json. */
    private static final Map<String,String> BUILT_IN=new LinkedHashMap<>();
    static{
        BUILT_IN.put("Хитмаркер","hit.marker");BUILT_IN.put("Клик","hit.click");BUILT_IN.put("Пульс","hit.pulse");
        BUILT_IN.put("Кристалл","hit.crystal");BUILT_IN.put("Бас","hit.bass");BUILT_IN.put("Неон","hit.neon");BUILT_IN.put("Стекло","hit.glass");
    }
    /** Initialised on first use: SoundEvents needs a bootstrapped registry, which unit tests do not have. */
    private static final class Vanilla {
        static final Set<SoundEvent> ATTACK=Set.of(SoundEvents.PLAYER_ATTACK_STRONG,SoundEvents.PLAYER_ATTACK_WEAK,SoundEvents.PLAYER_ATTACK_CRIT,
            SoundEvents.PLAYER_ATTACK_KNOCKBACK,SoundEvents.PLAYER_ATTACK_SWEEP,SoundEvents.PLAYER_ATTACK_NODAMAGE);
    }
    private static final String FILE_PREFIX="Файл: ";
    private final SoundClips clips=new SoundClips(folder());
    /** Outside the game (unit tests) Fabric has no config directory; fall back to a relative path that is never created. */
    private static java.nio.file.Path folder(){
        java.nio.file.Path config=null;
        try{config=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();}catch(RuntimeException ignored){}
        return (config==null?java.nio.file.Path.of("config"):config).resolve("velaclient").resolve("hitsounds");
    }
    private final DynamicChoiceSetting sound=setting(new DynamicChoiceSetting("sound","Звук","Встроенный звук Vela или файл из папки hitsounds","Хитмаркер","Хитмаркер",this::choices));
    private final NumberSetting volume=setting(new NumberSetting("volume","Громкость","Громкость звука",70,5,100,5));
    private final NumberSetting pitch=setting(new NumberSetting("pitch","Тон","Высота встроенного звука, %",100,50,200,5));
    private final BooleanSetting muteVanilla=setting(new BooleanSetting("mute_vanilla","Заглушить стандартный звук","Не проигрывать обычные звуки удара и ранения цели от ваших атак",true));
    /** Last attacked entity and when (System.nanoTime); read on the network thread, hence volatile. */
    private static volatile int lastTarget=Integer.MIN_VALUE;
    private static volatile double targetX,targetY,targetZ;
    private static volatile long targetUntil;
    private static final long HURT_WINDOW=400_000_000L;
    public HitSound(){
        super("hit_sound","Hit Sound","Свой звук при ударе по цели",ModuleCategory.UTILITIES);
        setting(new ActionSetting("open_folder","Открыть папку звуков",()->{
            try{java.nio.file.Files.createDirectories(clips.folder());}catch(java.io.IOException ignored){}
            net.minecraft.util.Util.getPlatform().openPath(clips.folder());
        }));
        pitch.visibleWhen(()->BUILT_IN.containsKey(sound.value()));
    }
    private List<String> choices(){
        var list=new ArrayList<String>(BUILT_IN.keySet());
        for(var file:clips.names())list.add(FILE_PREFIX+file);
        return list;
    }
    private String file(){var v=sound.value();return v.startsWith(FILE_PREFIX)?v.substring(FILE_PREFIX.length()):null;}
    @Override protected void onEnable(){var f=file();if(f!=null)clips.request(f);}
    @Override public void onClientTick(){var f=file();if(f!=null)clips.request(f);}

    public static void attacked(Entity target){
        var module=current();
        if(module==null||!(target instanceof LivingEntity living)||!living.isAlive())return;
        lastTarget=target.getId();targetX=target.getX();targetY=target.getY();targetZ=target.getZ();targetUntil=System.nanoTime()+HURT_WINDOW;
        float vol=module.volume.value().floatValue()/100;
        var builtIn=BUILT_IN.get(module.sound.value());
        if(builtIn!=null){
            var event=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("velaclient",builtIn));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event,module.pitch.value().floatValue()/100,vol));
            return;
        }
        var file=module.file();if(file==null)return;
        module.clips.play(file,vol*Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER));
    }
    /**
     * True for sounds the module replaces: vanilla attack sounds at the local player, and the hurt sound of the entity
     * the player just hit (servers play it at the target; many PvP servers disable attack sounds and only this remains).
     */
    public static boolean mutes(SoundEvent event,double x,double y,double z){
        var module=current();var player=Minecraft.getInstance().player;
        if(module==null||!module.muteVanilla.value()||player==null)return false;
        if(Vanilla.ATTACK.contains(event)&&player.distanceToSqr(x,y,z)<=2.5*2.5)return true;
        if(!hurtSound(event)||System.nanoTime()>targetUntil)return false;
        double dx=x-targetX,dy=y-targetY,dz=z-targetZ;
        return dx*dx+dy*dy+dz*dz<=3*3;
    }
    /** Entity-attached variant of {@link #mutes}. */
    public static boolean mutesEntity(SoundEvent event,int entityId){
        var module=current();
        if(module==null||!module.muteVanilla.value())return false;
        if(Vanilla.ATTACK.contains(event)){var p=Minecraft.getInstance().player;return p!=null&&p.getId()==entityId;}
        return entityId==lastTarget&&System.nanoTime()<=targetUntil&&hurtSound(event);
    }
    static boolean hurtSound(SoundEvent event){String path=event.location().getPath();return path.endsWith(".hurt")||path.contains(".hurt_")||path.endsWith("_hurt");}
    private static HitSound current(){
        var s=dev.sqwat.vela.VelaClient.services();
        return s!=null&&s.modules().byId("hit_sound") instanceof HitSound module&&module.enabled()?module:null;
    }
    public void close(){clips.close();}
}
