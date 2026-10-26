package dev.sqwat.vela;

import dev.sqwat.vela.module.utility.CommandEntries;
import dev.sqwat.vela.module.utility.CommandSchedule;
import dev.sqwat.vela.module.utility.Gamma;
import dev.sqwat.vela.module.*;
import dev.sqwat.vela.module.setting.BooleanSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CommandAutomationTest {
    @TempDir Path root;
    private CommandEntries.Entry entry(boolean interval) {
        return new CommandEntries.Entry(UUID.randomUUID(),interval,"Spawn","/spawn",65,2,true);
    }
    @Test void macroFiresOncePerPressAndNeverAfterHoldingInGui() {
        var s=new CommandSchedule();var e=entry(false);
        assertTrue(s.poll(e,0,true,true));assertFalse(s.poll(e,1,true,true));
        assertFalse(s.poll(e,2,true,false));assertTrue(s.poll(e,3,true,true));
        s.reset();assertFalse(s.poll(e,4,false,true));assertFalse(s.poll(e,5,true,true));
    }
    @Test void intervalWaitsResetsOnPauseAndNeverCatchesUp() {
        var s=new CommandSchedule();var e=entry(true);
        assertFalse(s.poll(e,0,true,false));assertFalse(s.poll(e,1999,true,false));
        assertTrue(s.poll(e,2000,true,false));assertTrue(s.poll(e,100000,true,false));
        assertFalse(s.poll(e,100001,true,false));assertFalse(s.poll(e,100002,false,false));
        assertFalse(s.poll(e,200000,true,false));assertTrue(s.poll(e,202000,true,false));
    }
    @Test void entriesRoundTripAndRejectMultilineCommands() {
        var entries=new CommandEntries(root);var e=entry(false);entries.save(e);
        assertEquals(e,new CommandEntries(root).all().getFirst());
        assertThrows(IllegalArgumentException.class,()->entries.save(new CommandEntries.Entry(e.id(),false,"Bad","/spawn\n/op user",65,2,true)));
        assertEquals(e,entries.all().getFirst());entries.remove(e.id());assertTrue(new CommandEntries(root).all().isEmpty());
    }
    @Test void frequentEntryCannotStarveOtherIntervals() {
        var s=new CommandSchedule();var first=new CommandEntries.Entry(UUID.randomUUID(),true,"A","/a",-1,1,true);
        var second=new CommandEntries.Entry(UUID.randomUUID(),true,"B","/b",-1,2,true);
        var entries=java.util.List.of(first,second);
        assertNull(s.interval(entries,0,true));assertEquals(first,s.interval(entries,1000,true));
        assertEquals(second,s.interval(entries,2000,true));assertNull(s.interval(entries,2001,true));
        assertEquals(first,s.interval(entries,3000,true));
    }
    @Test void smartGammaIsBoundedAndHookRegistered() throws Exception {
        assertEquals(.95f,Gamma.target(true,0));assertEquals(0,Gamma.target(true,15));
        assertEquals(1,Gamma.target(false,15));
        assertTrue(Files.readString(Path.of("src/main/resources/velaclient.mixins.json")).contains("\"world.GammaMixin\""));
    }
}
