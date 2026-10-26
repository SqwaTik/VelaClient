package dev.sqwat.vela.integration;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.*;
import dev.sqwat.vela.ui.HudTelemetry;

/** Loaded by Simple Voice Chat only when that mod is present. */
public final class VelaVoicePlugin implements VoicechatPlugin {
    @Override public String getPluginId(){return "vela_speakers";}
    @Override public void registerEvents(EventRegistration registration){
        registration.registerEvent(ClientReceiveSoundEvent.class,event->HudTelemetry.voice(event.getId(),event.getRawAudio()));
    }
}
