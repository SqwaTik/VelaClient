package dev.sqwat.vela.module.visual;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Writes a Death Recap replay to a video file. The replay advances exactly one video frame (1/30 s of replay time) per
 * captured game frame, so the file plays smoothly whatever the game's frame rate. Frames are scaled to at most 1280 px
 * wide and handed to a background thread: H.264 MP4 through FFmpeg when it is installed, otherwise a Motion-JPEG AVI
 * written in Java. At most a few frames wait in memory; capture pauses instead of growing the queue.
 */
public final class ReplayVideo {
    public static final int FPS=30,MAX_WIDTH=1280,QUEUE=6;
    private static ReplayVideo active;
    private final Path target;
    private final float speed;
    private final int total;
    private final BlockingQueue<byte[]> queue=new ArrayBlockingQueue<>(QUEUE);
    private final Consumer<Path> done;
    private Thread writer;
    private volatile Throwable failure;
    private int width=-1,height,captured,inFlight;
    private volatile boolean finished;
    private ReplayVideo(Path target,float speed,int total,Consumer<Path> done){this.target=target;this.speed=speed;this.total=total;this.done=done;}

    public static boolean running(){return active!=null;}
    public static float progress(){var v=active;return v==null?0:Math.min(1,v.captured/(float)Math.max(1,v.total));}
    /** Starts recording from the replay's current position to its end at {@code speed}; the file name has no extension. */
    public static void start(Path withoutExtension,float speed,Consumer<Path> done){
        int frames=Math.max(1,Math.round((DeathReplay.length()-DeathReplay.time())/20f/speed*FPS));
        active=new ReplayVideo(withoutExtension,speed,frames,done);
        DeathReplay.pauseForExport(true);
    }
    public static void cancel(){var v=active;if(v!=null){active=null;v.close(true);DeathReplay.pauseForExport(false);}}

    /** Called at the end of every rendered frame (GameRenderer.render TAIL): capture, then step the replay. */
    public static void frameEnd(){
        var v=active;if(v==null)return;
        if(v.failure!=null){cancel();return;}
        if(v.captured>=v.total){if(v.inFlight==0){active=null;v.close(false);DeathReplay.pauseForExport(false);}return;}
        if(v.inFlight+v.queue.size()>=QUEUE)return; // let the encoder catch up
        v.inFlight++;v.captured++;
        Screenshot.takeScreenshot(Minecraft.getInstance().gameRenderer.mainRenderTarget(),image->v.accept(image));
        DeathReplay.seek(DeathReplay.time()+20f*v.speed/FPS);
    }
    private void accept(NativeImage image){
        try(image){
            inFlight--;
            if(width<0){
                width=Math.min(MAX_WIDTH,image.getWidth())&~1;
                height=Math.round(image.getHeight()*(width/(float)image.getWidth()))&~1;
                startWriter();
            }
            byte[] rgb=new byte[width*height*3];
            try(var scaled=new NativeImage(width,height,false)){
                image.resizeSubRectTo(0,0,image.getWidth(),image.getHeight(),scaled);
                int i=0;
                for(int y=0;y<height;y++)for(int x=0;x<width;x++){int p=scaled.getPixel(x,y);rgb[i++]=(byte)(p>>16);rgb[i++]=(byte)(p>>8);rgb[i++]=(byte)p;}
            }
            queue.put(rgb);
        }catch(Throwable error){failure=error;}
    }
    private void startWriter(){
        writer=Thread.ofPlatform().daemon().name("Vela replay video").start(()->{
            Path file=null;
            try{
                file=Ffmpeg.available()?writeMp4():writeAvi();
                if(failure==null&&file!=null){Path result=file;Minecraft.getInstance().execute(()->done.accept(result));}
            }catch(Throwable error){failure=error;org.slf4j.LoggerFactory.getLogger("Vela/Recap").warn("Replay video failed",error);if(file!=null)try{Files.deleteIfExists(file);}catch(IOException ignored){}}
        });
    }
    private byte[] next()throws InterruptedException{
        while(true){
            var frame=queue.poll(200,java.util.concurrent.TimeUnit.MILLISECONDS);
            if(frame!=null)return frame;
            if(finished&&queue.isEmpty())return null;
        }
    }
    private Path writeMp4()throws Exception{
        Path file=Path.of(target+".mp4");
        var process=new ProcessBuilder(Ffmpeg.command(),"-y","-loglevel","error","-f","rawvideo","-pix_fmt","rgb24","-s",width+"x"+height,"-r",String.valueOf(FPS),"-i","-",
            "-c:v","libx264","-preset","veryfast","-crf","22","-pix_fmt","yuv420p","-movflags","+faststart",file.toString()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        try(var out=new BufferedOutputStream(process.getOutputStream(),1<<20)){
            for(byte[] frame;(frame=next())!=null;)out.write(frame);
        }
        if(process.waitFor()!=0)throw new IOException("ffmpeg exited with "+process.exitValue());
        return file;
    }
    private Path writeAvi()throws Exception{
        Path file=Path.of(target+".avi");
        try(var avi=new MjpegAvi(file,width,height,FPS)){
            var image=new java.awt.image.BufferedImage(width,height,java.awt.image.BufferedImage.TYPE_3BYTE_BGR);
            byte[] pixels=((java.awt.image.DataBufferByte)image.getRaster().getDataBuffer()).getData();
            for(byte[] frame;(frame=next())!=null;){
                for(int i=0;i<frame.length;i+=3){pixels[i]=frame[i+2];pixels[i+1]=frame[i+1];pixels[i+2]=frame[i];}
                avi.add(image);
            }
        }
        return file;
    }
    private void close(boolean aborted){
        finished=true;
        if(aborted&&writer!=null)writer.interrupt();
    }

    /** FFmpeg on PATH or in the usual WinGet location; probed once. */
    static final class Ffmpeg {
        private static String command;private static boolean probed;
        static synchronized boolean available(){if(!probed){probed=true;command=find();}return command!=null;}
        static String command(){return command;}
        private static String find(){
            for(String candidate:new String[]{"ffmpeg"}){
                try{var p=new ProcessBuilder(candidate,"-version").redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();if(p.waitFor()==0)return candidate;}catch(Exception ignored){}
            }
            var winget=Path.of(System.getenv().getOrDefault("LOCALAPPDATA",""),"Microsoft","WinGet","Packages");
            if(Files.isDirectory(winget))try(var walk=Files.walk(winget,5)){
                return walk.filter(f->f.getFileName().toString().equalsIgnoreCase("ffmpeg.exe")).map(Path::toString).findFirst().orElse(null);
            }catch(IOException ignored){}
            return null;
        }
    }

    /** Minimal Motion-JPEG AVI (RIFF, one video stream, idx1 index), playable by common players. */
    static final class MjpegAvi implements Closeable {
        private final RandomAccessFile out;
        private final int fps;
        private final java.util.List<long[]> index=new java.util.ArrayList<>();
        private long moviStart;private int frames;
        private final javax.imageio.ImageWriter writer=javax.imageio.ImageIO.getImageWritersByFormatName("jpeg").next();
        private final javax.imageio.ImageWriteParam param=writer.getDefaultWriteParam();
        MjpegAvi(Path file,int width,int height,int fps)throws IOException{
            this.fps=fps;
            out=new RandomAccessFile(file.toFile(),"rw");out.setLength(0);
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);param.setCompressionQuality(.85f);
            fourcc("RIFF");int32(0);fourcc("AVI ");
            fourcc("LIST");int32(4+8+56+8+4+8+56+8+40);fourcc("hdrl");
            fourcc("avih");int32(56);int32(1_000_000/fps);int32(0);int32(0);int32(0x10);int32(0);int32(0);int32(1);int32(0);int32(width);int32(height);int32(0);int32(0);int32(0);int32(0);
            fourcc("LIST");int32(4+8+56+8+40);fourcc("strl");
            fourcc("strh");int32(56);fourcc("vids");fourcc("MJPG");int32(0);int32(0);int32(0);int32(1);int32(fps);int32(0);int32(0);int32(0);int32(-1);int32(0);int16(0);int16(0);int16(width);int16(height);
            fourcc("strf");int32(40);int32(40);int32(width);int32(height);int16(1);int16(24);fourcc("MJPG");int32(width*height*3);int32(0);int32(0);int32(0);int32(0);
            fourcc("LIST");int32(0);moviStart=out.getFilePointer();fourcc("movi");
        }
        void add(java.awt.image.BufferedImage image)throws IOException{
            var bytes=new ByteArrayOutputStream();
            try(var stream=javax.imageio.ImageIO.createImageOutputStream(bytes)){writer.setOutput(stream);writer.write(null,new javax.imageio.IIOImage(image,null,null),param);}
            byte[] data=bytes.toByteArray();
            long offset=out.getFilePointer()-moviStart;
            fourcc("00dc");int32(data.length);out.write(data);if((data.length&1)!=0)out.write(0);
            index.add(new long[]{offset,data.length});frames++;
        }
        @Override public void close()throws IOException{
            long moviEnd=out.getFilePointer();
            fourcc("idx1");int32(index.size()*16);
            for(var e:index){fourcc("00dc");int32(0x10);int32((int)e[0]);int32((int)e[1]);}
            long end=out.getFilePointer();
            out.seek(4);int32((int)(end-8));
            out.seek(48);int32(frames);                 // avih total frames
            out.seek(140);int32(frames);                // strh length
            out.seek(moviStart-4);int32((int)(moviEnd-moviStart));
            out.close();writer.dispose();
        }
        private void fourcc(String s)throws IOException{out.write(s.getBytes(java.nio.charset.StandardCharsets.US_ASCII));}
        private void int32(int v)throws IOException{out.write(v);out.write(v>>8);out.write(v>>16);out.write(v>>24);}
        private void int16(int v)throws IOException{out.write(v);out.write(v>>8);}
    }
}
