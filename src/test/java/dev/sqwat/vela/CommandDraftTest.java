package dev.sqwat.vela;
import dev.sqwat.vela.module.CommandEntries;
import dev.sqwat.vela.ui.CommandDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class CommandDraftTest {
    @TempDir Path root;
    @Test void successiveCreatesNeverOverwriteAndExplicitSelectionEdits(){
        var entries=new CommandEntries(root);var draft=new CommandDraft();
        assertTrue(draft.save(entries,false,"One","/one",65,60,true));
        assertTrue(draft.save(entries,false,"Two","/two",66,60,true));
        assertEquals(2,entries.all().size());
        draft.select(entries.all().getFirst().id());
        assertFalse(draft.save(entries,false,"Edited","/three",65,60,true));
        assertEquals(2,entries.all().size());assertEquals("Edited",entries.all().getFirst().name());
        draft.fresh();assertTrue(draft.save(entries,true,"Timer","/timer",-1,20,true));
        assertEquals(3,entries.all().size());
    }
    @Test void editorBoundsScrollBeforeInterpolation() throws Exception {
        String source=java.nio.file.Files.readString(Path.of("src/main/java/dev/sqwat/vela/ui/screen/CommandEditorScreen.java"));
        assertFalse(source.contains("Float.MAX_VALUE"));
        int interpolation=source.indexOf("scroll=Motion.scroll");
        assertTrue(interpolation>=0 && source.indexOf("targetScroll=Math.clamp")<interpolation);
    }
}
