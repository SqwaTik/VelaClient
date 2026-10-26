package dev.sqwat.vela.module.utility;

import dev.sqwat.vela.audio.SoundClips;
import dev.sqwat.vela.module.ClientModule;
import dev.sqwat.vela.module.ModuleCategory;
import dev.sqwat.vela.module.setting.ActionSetting;
import dev.sqwat.vela.module.setting.DynamicChoiceSetting;
import dev.sqwat.vela.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Plays a user sound file when the player attacks a living entity. Sounds are .wav/.ogg files in
 * config/velaclient/hitsounds; the list follows the folder without a restart.
 */
public final class HitSound extends ClientModule {
    private static final String NO_FILES="Нет файлов в папке";
    private final SoundClips clips=new SoundClips(folder());
    /** Outside the game (unit tests) Fabric has no config directory; fall back to a relative path that is never created. */
    private static java.nio.file.Path folder(){
        java.nio.file.Path config=null;
        try{config=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();}catch(RuntimeException ignored){}
        return (config==null?java.nio.file.Path.of("config"):config).resolve("velaclient").resolve("hitsounds");
    }
    private final DynamicChoiceSetting sound=setting(new DynamicChoiceSetting("file","Звук","Файл .wav или .ogg из папки hitsounds",NO_FILES,clips::names));
    private final NumberSetting volume=setting(new NumberSetting("volume","Громкость","Громкость звука",70,5,100,5));
    public HitSound(){
        super("hit_sound","Hit Sound","Свой звук при ударе по цели",ModuleCategory.UTILITIES);
        setting(new ActionSetting("open_folder","Открыть папку звуков",()->{
            try{java.nio.file.Files.createDirectories(clips.folder());}catch(java.io.IOException ignored){}
            net.minecraft.util.Util.getPlatform().openPath(clips.folder());
        }));
    }
    @Override protected void onEnable(){clips.request(sound.selected());}
    @Override public void onClientTick(){clips.request(sound.selected());}
    public static void attacked(Entity target){
        var s=dev.sqwat.vela.VelaClient.services();
        if(s==null||!(s.modules().byId("hit_sound") instanceof HitSound module)||!module.enabled()||!(target instanceof LivingEntity living)||!living.isAlive())return;
        String name=module.sound.selected();if(name==null)return;
        float master=Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
        module.clips.play(name,module.volume.value().floatValue()/100*master);
    }
    public void close(){clips.close();}
}
