package dev.sqwat.vela.ui;
import dev.sqwat.vela.core.VelaServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import java.util.List;

/**
 * Interface sounds in three profiles. "Ритм" (default) is cut from the opening drum figure of an instrumental: a high
 * "pim" opens, a lower "pam" closes, a short pim switches tabs, a 45 ms tick follows sliders; every play varies the
 * pitch by up to 3%, so repeated clicks are never identical. "Мягкий" and "Классика" are the earlier Kenney sounds.
 */
public final class UiSounds {
    private UiSounds() {}
    public enum Event { OPEN,CLOSE,TAB,EXPAND,TOGGLE,FAVORITE,SLIDER }
    public static final String RHYTHM="Ритм",SOFT="Мягкий",CLASSIC="Классика";
    public static final List<String> PRESETS=List.of(RHYTHM,SOFT,CLASSIC);
    private static long lastPlayed;
    private static final java.util.Random RANDOM=new java.util.Random();

    /** The profile to use; names saved by earlier versions ("Soft", "Classic") map to their new names. */
    public static String preset(String saved){
        if(PRESETS.contains(saved))return saved;
        return "Soft".equals(saved)?SOFT:"Classic".equals(saved)?CLASSIC:RHYTHM;
    }
    public static String next(String saved){return PRESETS.get((PRESETS.indexOf(preset(saved))+1)%PRESETS.size());}

    public static void play(VelaServices services,Event event){
        if(services==null||(event!=Event.OPEN&&Minecraft.getInstance().gui.screen()==null))return;
        long now=System.nanoTime();if(now-lastPlayed<(event==Event.SLIDER?80_000_000L:25_000_000L))return;
        var config=services.configs().current();if(!config.uiSounds||config.uiSoundVolume<=0)return;
        lastPlayed=now;
        String preset=preset(config.uiSoundPreset);
        String id;float pitch,volume;
        if(preset.equals(RHYTHM)){
            String name=switch(event){case OPEN->"open";case CLOSE->"close";case TAB->"tab";case EXPAND->"expand";case SLIDER->"slider";default->"toggle";};
            id="ui.rhythm."+name;
            pitch=.97f+RANDOM.nextFloat()*.06f;
            volume=switch(event){case SLIDER->.35f;case TAB,TOGGLE,FAVORITE->.45f;default->.55f;};
        }else{
            var mapped=event==Event.EXPAND?Event.TAB:event==Event.FAVORITE?Event.TOGGLE:event;
            id="ui."+mapped.name().toLowerCase(java.util.Locale.ROOT);
            pitch=switch(mapped){case OPEN->.95f;case CLOSE->.8f;case TAB->1.1f;default->1;};
            if(preset.equals(CLASSIC))pitch*=.85f;
            volume=mapped==Event.TAB?.35f:.22f;
        }
        var sound=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("velaclient",id));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound,pitch,config.uiSoundVolume*volume));
    }
    /** A click in Vela's own screens (toggles, buttons, the "Прослушать" preview). */
    public static void play(VelaServices services, boolean enabled) {
        play(services,Event.TOGGLE);
    }
}
