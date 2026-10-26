package dev.sqwat.vela;
import dev.sqwat.vela.config.JsonFileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import static org.junit.jupiter.api.Assertions.*;
class StoreDeduplicationTest {
    @TempDir Path root;
    @Test void unchangedSaveDoesNotTouchDisk()throws Exception{Path path=root.resolve("a.json");var store=new JsonFileStore<String>(path,String.class,()->"");store.save("same");var stamp=FileTime.fromMillis(1000);Files.setLastModifiedTime(path,stamp);store.save("same");assertEquals(stamp,Files.getLastModifiedTime(path));store.save("changed");assertEquals("changed",store.load());}
}
