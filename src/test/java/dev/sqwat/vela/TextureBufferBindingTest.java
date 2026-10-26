package dev.sqwat.vela;
import dev.sqwat.vela.render.perf.TextureBufferBinding;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TextureBufferBindingTest {
    @Test void onlyIdenticalCompletedAttachmentsCanBeSkipped(){
        var c=new TextureBufferBinding();assertFalse(c.matches(1,2,3));
        c.record(1,2,3);assertTrue(c.matches(1,2,3));
        assertFalse(c.matches(2,2,3));assertFalse(c.matches(1,3,3));assertFalse(c.matches(1,2,4));
        c.clear();assertFalse(c.matches(1,2,3));
        c.record(0,2,3);assertFalse(c.matches(0,2,3));
        c.record(1,2,0);assertFalse(c.matches(1,2,0));
    }
    @Test void differentAttachmentReplacesPreviousRecord(){
        var c=new TextureBufferBinding();c.record(1,2,3);c.record(1,2,4);
        assertFalse(c.matches(1,2,3));assertTrue(c.matches(1,2,4));
    }
}
