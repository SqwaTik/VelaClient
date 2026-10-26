package dev.sqwat.vela;
import dev.sqwat.vela.ui.TextEdit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TextEditTest {
 @Test void selectionDoesNotDeleteUntilEdited(){var e=new TextEdit();e.set("hello");e.selectAll();assertEquals("hello",e.text());assertEquals("hello",e.selected());e.insert("world",100);assertEquals("world",e.text());}
 @Test void editsInsideText(){var e=new TextEdit();e.set("abcd");e.move(2,false);e.insert("!",20);assertEquals("ab!cd",e.text());e.erase(false);assertEquals("abcd",e.text());e.erase(true);assertEquals("abd",e.text());}
 @Test void unicodeSelection(){var e=new TextEdit();e.set("A😀B");e.move(3,false);e.erase(false);assertEquals("AB",e.text());}
}
