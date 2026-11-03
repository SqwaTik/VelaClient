package dev.sqwat.vela.media;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeatMeterTest {
    private static float[] spectrum(float low,float rest){
        float[] b=new float[20];
        for(int i=0;i<b.length;i++)b[i]=i<4?low:rest;
        return b;
    }
    @Test void kickAfterSteadyMusicIsABeat(){
        var meter=new BeatMeter();long t=1000;
        for(int i=0;i<40;i++){meter.feed(spectrum(.6f,.5f),t);t+=40;}
        assertTrue(meter.pulse(t)<.05f,"no beat in steady sound");
        meter.feed(spectrum(.85f,.5f),t);
        assertTrue(meter.pulse(t)>.5f,"a jump of the low bands is a beat");
        assertTrue(meter.pulse(t+600)<.05f,"the pulse fades");
        for(float h:meter.heights())assertTrue(h>=0&&h<=1);
    }
    @Test void barsShowChangeNotJustLoudness(){
        var meter=new BeatMeter();long t=1000;
        for(int i=0;i<20;i++){meter.feed(spectrum(.8f,.8f),t);t+=40;}
        float loud=meter.heights()[10];
        // 0.1 down in the helper's scale is 5.6 dB: the bar must drop visibly, not by a tenth.
        meter.feed(spectrum(.8f,.7f),t);
        assertTrue(meter.heights()[10]<loud*.65f,meter.heights()[10]+" vs "+loud);
    }
    @Test void magnitudeUndoesTheHelpersScale(){
        assertEquals(1,BeatMeter.magnitude(62/56f,0,20),1e-3);
        assertEquals(.5,BeatMeter.magnitude((62-6.0206f)/56f,0,20),1e-3);
    }
}
