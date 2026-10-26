package dev.sqwat.vela.render.perf;

import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** Opt-in bounded profiling. No environment, account, file-content or network events. */
public final class PerformanceRecording {
    private static volatile Recording active;
    @jdk.jfr.Name("vela.SlowFrame")
    @jdk.jfr.Label("Vela slow frame interval")
    @jdk.jfr.StackTrace(false)
    public static final class SlowFrame extends jdk.jfr.Event {
        @jdk.jfr.Label("Frame interval (ms)") public double milliseconds;
    }
    private PerformanceRecording() {}
    public static synchronized void start() {
        cancel();
        Recording recording=new Recording();
        try {
            recording.setName("Vela performance");
            recording.setMaxSize(64L*1024*1024);
            recording.setDuration(Duration.ofMinutes(5));
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
            recording.start();active=recording;
        } catch(RuntimeException error) {recording.close();throw error;}
    }
    public static synchronized void cancel(){if(active!=null){active.close();active=null;}}
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
                Files.createDirectories(directory);
                Path output=directory.resolve("vela-"+System.currentTimeMillis()+".jfr");
                recording.dump(output);
                org.slf4j.LoggerFactory.getLogger("Vela/Performance").info("Profile saved: {}",output);
            }catch(Exception error){org.slf4j.LoggerFactory.getLogger("Vela/Performance").warn("Profile save failed",error);}
        });
    }
}
