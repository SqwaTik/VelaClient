package dev.sqwat.vela.render.perf;

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** Opt-in bounded profiling. No environment, account, file-content or network events. */
public final class PerformanceRecording {
    private static volatile Recording active;
    private static volatile boolean rolling;
    /** Saved profiles kept in the output directory; older ones are deleted. */
    static final int KEEP=24;
    @jdk.jfr.Name("vela.SlowFrame")
    @jdk.jfr.Label("Vela slow frame interval")
    @jdk.jfr.StackTrace(false)
    public static final class SlowFrame extends jdk.jfr.Event {
        @jdk.jfr.Label("Frame interval (ms)") public double milliseconds;
    }
    private PerformanceRecording() {}
    /** A five-minute profile, or with {@code rolling} a continuous one that only keeps the last three minutes. */
    public static synchronized void start(boolean rolling) {
        cancel();
        Recording recording=new Recording();
        try {
            recording.setName("Vela performance");
            recording.setMaxSize(64L*1024*1024);
            if(rolling){recording.setToDisk(true);recording.setMaxAge(Duration.ofMinutes(3));}
            else recording.setDuration(Duration.ofMinutes(5));
            recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(20)).withStackTrace();
            recording.enable("jdk.NativeMethodSample").withPeriod(Duration.ofMillis(20)).withStackTrace();
            recording.enable("jdk.SafepointBegin").withThreshold(Duration.ofMillis(2));
            recording.enable("jdk.SafepointEnd").withThreshold(Duration.ofMillis(2));
            recording.enable(SlowFrame.class);
            recording.enable("jdk.ObjectAllocationSample").withStackTrace();
            recording.enable("jdk.GCPhasePause").withThreshold(Duration.ZERO);
            recording.enable("jdk.GarbageCollection");
            recording.enable("jdk.GCHeapSummary");
            recording.enable("jdk.PhysicalMemory").withPeriod(Duration.ofSeconds(1));
            recording.enable("jdk.CPULoad").withPeriod(Duration.ofSeconds(1));
            recording.enable("jdk.JavaMonitorEnter").withThreshold(Duration.ofMillis(2)).withStackTrace();
            recording.enable("jdk.ThreadPark").withThreshold(Duration.ofMillis(2)).withStackTrace();
            recording.start();active=recording;PerformanceRecording.rolling=rolling;
        } catch(RuntimeException error) {recording.close();throw error;}
    }
    public static synchronized void cancel(){if(active!=null){active.close();active=null;}}
    public static boolean running(){return active!=null;}
    public static void frame(double milliseconds){
        if(active==null||milliseconds<4)return;
        SlowFrame event=new SlowFrame();event.milliseconds=milliseconds;event.commit();
    }
    public static synchronized void finish(Path directory){
        Recording recording=active;active=null;
        if(recording==null)return;
        // Disk output must not stall the render thread.
        Thread.ofPlatform().daemon().name("Vela performance save").start(()->{
            try(recording){
                if(recording.getState()==RecordingState.RUNNING)recording.stop();
                save(recording,directory,"");
            }catch(Exception error){org.slf4j.LoggerFactory.getLogger("Vela/Performance").warn("Profile save failed",error);}
        });
    }
    /** Saves what the running recording holds (the last minutes when rolling) without stopping it. */
    public static synchronized void snapshot(Path directory,String label){
        Recording recording=active;
        if(recording==null||!rolling)return;
        Thread.ofPlatform().daemon().name("Vela performance snapshot").start(()->{
            try{save(recording,directory,"-"+label);}
            catch(Exception error){org.slf4j.LoggerFactory.getLogger("Vela/Performance").warn("Profile snapshot failed",error);}
        });
    }
    private static void save(Recording recording,Path directory,String suffix)throws Exception{
        Files.createDirectories(directory);
        Path output=directory.resolve("vela-"+System.currentTimeMillis()+suffix+".jfr");
        recording.dump(output);
        var report=ProfileReport.write(output);
        org.slf4j.LoggerFactory.getLogger("Vela/Performance").info("Profile saved: {} (report {})",output,report.getFileName());
        prune(directory);
    }
    /** Keeps the newest {@link #KEEP} profiles and their reports. */
    static void prune(Path directory){
        try(var files=Files.list(directory)){
            var profiles=files.filter(p->p.getFileName().toString().matches("vela-\\d+.*\\.jfr")).sorted(java.util.Comparator.comparing((Path p)->p.getFileName().toString()).reversed()).toList();
            for(var old:profiles.subList(Math.min(KEEP,profiles.size()),profiles.size())){
                Files.deleteIfExists(old);Files.deleteIfExists(old.resolveSibling(old.getFileName().toString().replace(".jfr",".txt")));
            }
        }catch(Exception ignored){}
    }
}
