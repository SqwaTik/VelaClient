package dev.sqwat.vela.audio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User sound files (WAV or OGG Vorbis, detected by header, not extension) played through javax.sound.
 * Decoding and Clip opening run on a daemon thread; the render thread only starts an already-open clip.
 */
public final class SoundClips implements AutoCloseable {
    private static final int VOICES=4;
    private static final long MAX_BYTES=8L*1024*1024;
    private final Path folder;
    private final ExecutorService loader=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Vela sound loader");t.setDaemon(true);return t;});
    private volatile Loaded loaded;
    private String requested;
    private List<String> names=List.of();
    private long scanned;
    private record Loaded(String name,Clip[] voices){}

    public SoundClips(Path folder){this.folder=folder;}
    public Path folder(){return folder;}

    /** Sound names (file names without extension); rescanned at most every two seconds. */
    public synchronized List<String> names(){
        long now=System.nanoTime();
        if(scanned!=0&&now-scanned<2_000_000_000L)return names;
        scanned=now;
        var found=new ArrayList<String>();
        try{
            if(Files.isDirectory(folder))try(var files=Files.list(folder)){
                for(var file:files.toList()){
                    String n=file.getFileName().toString(),lower=n.toLowerCase(Locale.ROOT);
                    if(Files.isRegularFile(file)&&(lower.endsWith(".wav")||lower.endsWith(".ogg")))found.add(n.substring(0,n.length()-4));
                }
            }
        }catch(IOException ignored){}
        found.sort(String.CASE_INSENSITIVE_ORDER);
        return names=List.copyOf(found);
    }

    /** Starts the sound if it is loaded; otherwise requests loading and returns without blocking. */
    public void play(String name,float volume){
        var current=loaded;
        if(current==null||!current.name.equals(name)){request(name);return;}
        Clip voice=null;
        for(var clip:current.voices)if(!clip.isRunning()){voice=clip;break;}
        if(voice==null)voice=current.voices[0];
        gain(voice,volume);
        voice.stop();voice.setFramePosition(0);voice.start();
    }
    /** Preloads the selected sound so the first hit already plays. */
    public synchronized void request(String name){
        if(name==null||name.equals(requested))return;
        requested=name;
        loader.execute(()->{
            Loaded next=null;
            try{next=load(name);}catch(Exception error){org.slf4j.LoggerFactory.getLogger("Vela").warn("Hit sound {} could not be loaded: {}",name,error.toString());}
            var previous=loaded;loaded=next;
            if(previous!=null)for(var clip:previous.voices)clip.close();
        });
    }
    private Loaded load(String name)throws Exception{
        Path file=null;
        for(String ext:new String[]{".wav",".ogg",".WAV",".OGG"}){var p=folder.resolve(name+ext);if(Files.isRegularFile(p)){file=p;break;}}
        if(file==null)throw new IOException("missing file");
        if(Files.size(file)>MAX_BYTES)throw new IOException("file larger than 8 MB");
        byte[] data=Files.readAllBytes(file);
        var pcm=decode(data);
        Clip[] voices=new Clip[VOICES];
        for(int i=0;i<VOICES;i++){voices[i]=AudioSystem.getClip();voices[i].open(pcm.format,pcm.bytes,0,pcm.bytes.length);}
        return new Loaded(name,voices);
    }
    record Pcm(AudioFormat format,byte[] bytes){}
    static Pcm decode(byte[] data)throws IOException,UnsupportedAudioFileException{
        if(data.length>=4&&data[0]=='O'&&data[1]=='g'&&data[2]=='g'&&data[3]=='S')return decodeVorbis(data);
        try(var in=AudioSystem.getAudioInputStream(new ByteArrayInputStream(data))){
            var source=in.getFormat();
            var target=new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,source.getSampleRate(),16,source.getChannels(),source.getChannels()*2,source.getSampleRate(),false);
            try(var pcm=AudioSystem.getAudioInputStream(target,in)){return new Pcm(target,pcm.readAllBytes());}
        }
    }
    private static Pcm decodeVorbis(byte[] data)throws IOException{
        var encoded=org.lwjgl.system.MemoryUtil.memAlloc(data.length);
        try(var stack=org.lwjgl.system.MemoryStack.stackPush()){
            encoded.put(data).flip();
            var channels=stack.mallocInt(1);var rate=stack.mallocInt(1);
            var samples=org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory(encoded,channels,rate);
            if(samples==null)throw new IOException("invalid OGG Vorbis data");
            try{
                byte[] bytes=new byte[samples.remaining()*2];
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(samples);
                int ch=channels.get(0);
                return new Pcm(new AudioFormat(rate.get(0),16,ch,true,false),bytes);
            }finally{org.lwjgl.system.libc.LibCStdlib.free(samples);}
        }finally{org.lwjgl.system.MemoryUtil.memFree(encoded);}
    }
    private static void gain(Clip clip,float volume){
        if(!clip.isControlSupported(FloatControl.Type.MASTER_GAIN))return;
        var control=(FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
        float db=(float)(20*Math.log10(Math.max(1e-4,volume)));
        control.setValue(Math.clamp(db,control.getMinimum(),control.getMaximum()));
    }
    @Override public void close(){
        loader.shutdownNow();
        var current=loaded;loaded=null;
        if(current!=null)for(var clip:current.voices)clip.close();
    }
}
