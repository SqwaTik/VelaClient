package dev.sqwat.vela;

import dev.sqwat.vela.zako.particle.Workers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicIntegerArray;
import static org.junit.jupiter.api.Assertions.*;

class ZakoWorkersTest {
    @AfterEach void cleanup(){Workers.shutdown();}
    @Test void everyBlockRunsExactlyOnceAndRestartWorks(){
        for(int pass=0;pass<2;pass++){
            var counts=new AtomicIntegerArray(513);
            assertNull(Workers.run(counts.length(),counts::incrementAndGet));
            for(int n=0;n<counts.length();n++)assertEquals(1,counts.get(n));
            Workers.shutdown();
        }
    }
    @Test void streamingPublicationJoinsAllWork(){
        var counts=new AtomicIntegerArray(129);
        var stream=Workers.stream(counts::incrementAndGet);
        stream.publish(17);assertNull(stream.await(64));stream.publish(129);
        assertNull(stream.finish(129));
        for(int n=0;n<counts.length();n++)assertEquals(1,counts.get(n));
        assertThrows(IllegalStateException.class,()->stream.publish(130));
    }
    @Test void workerFailureIsReturnedAtBarrier(){
        var failure=new IllegalStateException("test worker failure");
        assertSame(failure,Workers.run(64,n->{throw failure;}));
        var stream=Workers.stream(n->{throw failure;});stream.publish(32);
        assertSame(failure,stream.finish(32));
    }
    @Test void streamingBatchesModelsAndFlushesShortTail(){
        var counts=new AtomicIntegerArray(65);
        var stream=Workers.stream(counts::incrementAndGet);
        for(int i=1;i<=65;i++)stream.publish(i);
        assertEquals(8,stream.submittedTasks());
        assertNull(stream.finish(65));assertEquals(9,stream.submittedTasks());
        for(int i=0;i<65;i++)assertEquals(1,counts.get(i));
    }
}
