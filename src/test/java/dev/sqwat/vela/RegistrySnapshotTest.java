package dev.sqwat.vela;
import dev.sqwat.vela.module.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RegistrySnapshotTest {
    private static final class Entry extends ClientModule{Entry(String id){super(id,id,"description",ModuleCategory.HUD);}}
    @Test void immutableSnapshotIsReusedAndInvalidated(){var r=new ModuleRegistry();r.register(new Entry("first"));var old=r.all();assertSame(old,r.all());assertThrows(UnsupportedOperationException.class,old::clear);r.register(new Entry("second"));assertEquals(1,old.size());assertEquals(2,r.all().size());}
    @Test void searchCacheInvalidatesOnRegistration(){var r=new ModuleRegistry();assertTrue(r.findAll("new").isEmpty());r.register(new Entry("new"));assertEquals(1,r.findAll("new").size());assertSame(r.findAll("new"),r.findAll("new"));}
}
