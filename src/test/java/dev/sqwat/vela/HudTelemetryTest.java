package dev.sqwat.vela;
import dev.sqwat.vela.ui.HudTelemetry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class HudTelemetryTest {
    @Test void emptyAudioHidesSpeakerAndLevelsStayBounded(){
        HudTelemetry.clear();var id=java.util.UUID.randomUUID();
        HudTelemetry.voice(id,new short[]{32767,-32768});
        assertEquals(1,HudTelemetry.speakers().size());
        assertTrue(HudTelemetry.speakers().getFirst().level()<=1);
        HudTelemetry.voice(id,new short[0]);assertTrue(HudTelemetry.speakers().isEmpty());
        HudTelemetry.voice(id,new short[]{0,0});assertTrue(HudTelemetry.speakers().isEmpty());
    }
    @Test void serverEstimateRequiresSamplesAndResets(){
        HudTelemetry.clear();long now=System.nanoTime();
        assertTrue(Double.isNaN(HudTelemetry.tps()));
        HudTelemetry.serverTime(100,now-2_000_000_000L);HudTelemetry.serverTime(120,now);
        assertEquals(10,HudTelemetry.tps(),.001);
        HudTelemetry.clear();assertTrue(Double.isNaN(HudTelemetry.tps()));
    }
}
