package dev.sqwat.vela;
import dev.sqwat.vela.zako.entity.WorkingSetWindow;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WorkingSetWindowTest {
    @Test void oversizedPageShrinksWhileSmallDemandContinues(){
        assertEquals(1<<20,WorkingSetWindow.capacityAfterWindow(32<<20,12000));
        assertEquals(3<<20,WorkingSetWindow.capacityAfterWindow(32<<20,(2<<20)+1));
        assertEquals(4<<20,WorkingSetWindow.capacityAfterWindow(4<<20,3<<20));
    }
    @Test void highWaterMarkExpiresWithoutDiscardingRecentDemand(){
        var window=new WorkingSetWindow();assertEquals(-1,window.retainedPages(0));
        window.used(30);window.used(2);assertEquals(-1,window.retainedPages(4_999_999_999L));
        assertEquals(30,window.retainedPages(5_000_000_000L));
        window.used(2);assertEquals(2,window.retainedPages(10_000_000_000L));
        assertEquals(0,window.retainedPages(15_000_000_000L));
        window.clear();assertEquals(-1,window.retainedPages(20_000_000_000L));
    }
}
