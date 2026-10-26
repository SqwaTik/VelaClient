package dev.sqwat.vela.core;

import org.junit.jupiter.api.Test;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

class SoundClipsTest {
    @Test void decodesPcmWavToSigned16() throws Exception {
        var format=new AudioFormat(48000,16,1,true,false);
        byte[] samples=new byte[4800*2];
        for(int i=0;i<4800;i++){short v=(short)(Math.sin(i/10.0)*12000);samples[i*2]=(byte)v;samples[i*2+1]=(byte)(v>>8);}
        var out=new ByteArrayOutputStream();
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(samples),format,4800),AudioFileFormat.Type.WAVE,out);
        var pcm=SoundClips.decode(out.toByteArray());
        assertEquals(16,pcm.format().getSampleSizeInBits());
        assertEquals(1,pcm.format().getChannels());
        assertArrayEquals(samples,pcm.bytes());
    }
    @Test void oggHeaderIsDetectedRegardlessOfExtension() {
        // Not valid Vorbis: must be routed to the Vorbis decoder and rejected, not parsed as WAV.
        byte[] fake={'O','g','g','S',0,0,0,0};
        var error=assertThrows(Exception.class,()->SoundClips.decode(fake));
        assertTrue(error.getMessage().contains("Vorbis"),error.getMessage());
    }
    @Test void listsOnlyWavAndOggByBaseName() throws Exception {
        var dir=Files.createTempDirectory("vela-sounds");
        Files.write(dir.resolve("kozel.wav"),new byte[]{1});Files.write(dir.resolve("5sim.WAV"),new byte[]{1});
        Files.write(dir.resolve("notes.txt"),new byte[]{1});Files.write(dir.resolve("beep.ogg"),new byte[]{1});
        try(var clips=new SoundClips(dir)){assertEquals(java.util.List.of("5sim","beep","kozel"),clips.names());}
    }
}
