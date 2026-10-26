package dev.sqwat.vela;
import dev.sqwat.vela.ui.input.TextEdit;
import dev.sqwat.vela.ui.input.TextInput;
import dev.sqwat.vela.ui.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TextInputTest {
    static class Clipboard implements TextInput.Clipboard {String text="";public String get(){return text;}public void set(String s){text=s;}}
    @Test void sharedCutPasteAndFilter(){var edit=new TextEdit();var clip=new Clipboard();edit.set("Player");TextInput.key(edit,65,2,16,s->s,clip);TextInput.key(edit,88,2,16,s->s,clip);assertEquals("",edit.text());assertEquals("Player",clip.text);clip.text="a!b\nc";TextInput.key(edit,86,2,16,s->s.replaceAll("[^a-z]",""),clip);assertEquals("abc",edit.text());}
    @Test void shiftedSelectionAndDelete(){var edit=new TextEdit();var clip=new Clipboard();edit.set("ab");TextInput.key(edit,263,1,16,s->s,clip);assertEquals("b",edit.selected());TextInput.key(edit,261,0,16,s->s,clip);assertEquals("a",edit.text());}
}
