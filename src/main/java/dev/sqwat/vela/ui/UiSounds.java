package dev.sqwat.vela.ui;
import dev.sqwat.vela.core.VelaServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
public final class UiSounds {
    private UiSounds() {}
    public enum Event { OPEN,CLOSE,TAB,EXPAND,TOGGLE,FAVORITE,SLIDER }
    private static long lastPlayed;
    public static void play(VelaServices services,Event event){
        if(event==Event.EXPAND)event=Event.TAB;
        if(event!=Event.OPEN&&event!=Event.CLOSE&&event!=Event.TAB&&event!=Event.SLIDER)return;
        if(services==null||(event!=Event.OPEN&&Minecraft.getInstance().gui.screen()==null))return;
        long now=System.nanoTime();if(now-lastPlayed<(event==Event.SLIDER?80_000_000L:25_000_000L))return;
        var config=services.configs().current();if(!config.uiSounds||config.uiSoundVolume<=0)return;
        lastPlayed=now;
        float pitch=switch(event){case OPEN->.95f;case CLOSE->.8f;case TAB->1.1f;default->1;};
        if(!config.uiSoundPreset.equals("Soft"))pitch*=.85f;
        var sound=SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("velaclient","ui."+event.name().toLowerCase(java.util.Locale.ROOT)));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound,pitch,config.uiSoundVolume*(event==Event.TAB?.35f:.22f)));
    }
    public static void play(VelaServices services, boolean enabled) {
        play(services,Event.TOGGLE);
    }
}
