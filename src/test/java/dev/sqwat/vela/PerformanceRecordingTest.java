package dev.sqwat.vela;

import dev.sqwat.vela.render.PerformanceRecording;
import jdk.jfr.FlightRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PerformanceRecordingTest {
    @TempDir Path directory;
    @Test void recordingIsBoundedAndDoesNotEnableEnvironmentEvents() {
        try {
            PerformanceRecording.start();
            PerformanceRecording.start();
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
}
