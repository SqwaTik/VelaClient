package dev.sqwat.vela;

import dev.sqwat.vela.render.perf.PerformanceRecording;
import jdk.jfr.FlightRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PerformanceRecordingTest {
    @TempDir Path directory;
    @Test void recordingIsBoundedAndDoesNotEnableEnvironmentEvents() {
        try {
            PerformanceRecording.start(false);
            PerformanceRecording.start(false);
            var recordings=FlightRecorder.getFlightRecorder().getRecordings().stream().filter(r->r.getName().equals("Vela performance")).toList();
            assertEquals(1,recordings.size());
            var r=recordings.getFirst();
            assertEquals(java.time.Duration.ofMinutes(5),r.getDuration());
            assertEquals(64L*1024*1024,r.getMaxSize());
            assertFalse(r.getSettings().containsKey("jdk.InitialEnvironmentVariable#enabled"));
            assertEquals("true",r.getSettings().get("jdk.ExecutionSample#enabled"));
        } finally {PerformanceRecording.cancel();}
        assertTrue(FlightRecorder.getFlightRecorder().getRecordings().stream().noneMatch(r->r.getName().equals("Vela performance")));
    }
    @Test void rollingRecordingKeepsOnlyRecentMinutesAndSnapshotsWriteAReport()throws Exception{
        try{
            PerformanceRecording.start(true);
            var r=FlightRecorder.getFlightRecorder().getRecordings().stream().filter(x->x.getName().equals("Vela performance")).findFirst().orElseThrow();
            assertNull(r.getDuration());assertEquals(java.time.Duration.ofMinutes(3),r.getMaxAge());
            for(int i=0;i<40;i++){PerformanceRecording.frame(i%2==0?60:20);busy();}
            PerformanceRecording.snapshot(directory,"stall");
            Path report=null;
            for(int wait=0;wait<200&&report==null;wait++){
                Thread.sleep(50);
                try(var files=java.nio.file.Files.list(directory)){report=files.filter(f->f.toString().endsWith("-stall.txt")).findFirst().orElse(null);}
            }
            assertNotNull(report,"snapshot report");
            String text=java.nio.file.Files.readString(report);
            assertTrue(text.contains("Frames over 16.7 ms: 40"),text);assertTrue(text.contains("over 50 ms: 20"),text);
            assertTrue(PerformanceRecording.running(),"a snapshot does not stop the rolling recording");
        }finally{PerformanceRecording.cancel();}
    }
    @Test void automaticProfilingNeedsTheMarkerFileOrVariable()throws Exception{
        assertFalse(dev.sqwat.vela.render.perf.PerformanceCaptureAccess.automatic(directory,null));
        assertTrue(dev.sqwat.vela.render.perf.PerformanceCaptureAccess.automatic(directory,"1"));
        java.nio.file.Files.createFile(directory.resolve("profiler-auto"));
        assertTrue(dev.sqwat.vela.render.perf.PerformanceCaptureAccess.automatic(directory,null));
    }
    private static double sink;
    private static void busy(){for(int i=0;i<200_000;i++)sink+=Math.sqrt(i);}
}
