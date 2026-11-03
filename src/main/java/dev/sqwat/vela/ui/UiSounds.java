package dev.sqwat.vela.ui;
import dev.sqwat.vela.core.VelaServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import java.util.List;

/**
 * Interface sounds in two profiles. "Ритм" (default) is cut from the first half second of an instrumental (two drum
 * hits): every click, in Vela's screens and on vanilla buttons alike, plays one of those two hits picked at random with
 * the pitch varied by up to 8%, so repeated clicks are never identical; a 40 ms tick follows sliders. The clips must be
 * audio-only Ogg Vorbis: one with the mp3's cover art left in as a video stream does not decode, and the profile was
 * silent. "Классика" is the earlier Kenney set.
 */
public final class UiSounds {
    private UiSounds() {}
    public enum Event { OPEN,CLOSE,TAB,EXPAND,TOGGLE,FAVORITE,SLIDER }
    public static final String RHYTHM="Ритм",CLASSIC="Классика";
    public static final List<String> PRESETS=List.of(RHYTHM,CLASSIC);
    private static long lastPlayed;
    private static final java.util.Random RANDOM=new java.util.Random();

    /** The profile to use; "Classic" from earlier versions maps to its new name, the removed soft profile to Ритм. */
    public static String preset(String saved){
        if(PRESETS.contains(saved))return saved;
        return "Classic".equals(saved)?CLASSIC:RHYTHM;
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
            String name=event==Event.SLIDER?"slider":RANDOM.nextBoolean()?"open":"close";
            id="ui.rhythm."+name;
            pitch=.92f+RANDOM.nextFloat()*.16f;
            volume=event==Event.SLIDER?.35f:.55f;
        }else{
            var mapped=event==Event.EXPAND?Event.TAB:event==Event.FAVORITE?Event.TOGGLE:event;
            id="ui."+mapped.name().toLowerCase(java.util.Locale.ROOT);
            pitch=.85f*(switch(mapped){case OPEN->.95f;case CLOSE->.8f;case TAB->1.1f;default->1f;});
            volume=mapped==Event.TAB?.35f:.22f;
        }
        var sound=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("velaclient",id));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound,pitch,config.uiSoundVolume*volume));
    }
    /** Vanilla button clicks: true when Ритм played its own sound instead, so the vanilla click is skipped. */
    public static boolean replaceVanillaClick(){
        var services=dev.sqwat.vela.VelaClient.services();
        if(services==null)return false;
        var config=services.configs().current();
        if(!config.uiSounds||!preset(config.uiSoundPreset).equals(RHYTHM))return false;
        play(services,Event.TOGGLE);
        return true;
    }
    /** A click in Vela's own screens (toggles, buttons, the "Прослушать" preview). */
    public static void play(VelaServices services, boolean enabled) {
        play(services,Event.TOGGLE);
    }
}
