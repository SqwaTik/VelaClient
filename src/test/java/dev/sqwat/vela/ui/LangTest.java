package dev.sqwat.vela.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class LangTest {
    @AfterEach void reset(){Lang.set(Lang.Language.EN);}
    @Test void englishIsDefaultAndTranslatesKnownText(){
        assertEquals(Lang.Language.EN,Lang.current());
        assertTrue(Lang.size()>800);
        assertEquals("Search",Lang.tr("Поиск"));
        assertEquals("Hotbar",Lang.tr("Хотбар"));
    }
    @Test void templatesCoverRuntimeText(){
        assertEquals("5 min ago",Lang.tr("5 мин назад"));
        assertEquals("Connecting to mc.server.net in 3 s",Lang.tr("Подключение к mc.server.net через 3 с"));
    }
    @Test void unknownAndLatinTextIsUnchanged(){
        assertEquals("Wasted",Lang.tr("Wasted"));
        assertEquals("Неизвестная строка 42",Lang.tr("Неизвестная строка 42"));
    }
    @Test void russianModeShowsOriginal(){
        Lang.set(Lang.Language.RU);
        assertEquals("Поиск",Lang.tr("Поиск"));
    }
    /** Every Russian interface string literal in the sources must have an English entry. */
    @Test void everyInterfaceStringIsTranslated()throws Exception{
        var cyrillic=Pattern.compile("[\\u0400-\\u04FF]");
        var literal=Pattern.compile("\"((?:[^\"\\\\\\n]|\\\\.)*)\"");
        var missing=new TreeSet<String>();
        try(var files=Files.walk(Path.of("src/main/java/dev/sqwat/vela"))){
            for(var file:(Iterable<Path>)files.filter(p->p.toString().endsWith(".java"))::iterator){
                for(String line:Files.readAllLines(file)){
                    String t=line.strip();
                    if(t.startsWith("//")||t.startsWith("*")||t.startsWith("/*"))continue;
                    var m=literal.matcher(line);
                    while(m.find()){
                        String s=m.group(1).replace("\\\"","\"").replace("\\n","\n");
                        if(!cyrillic.matcher(s).find()||s.contains("(?")||s.equals("Файл: ")||s.startsWith("Метка ")||s.equals("Русский"))continue;
                        if(Lang.tr(s).equals(s))missing.add(file.getFileName()+": "+s);
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(),"Untranslated: "+missing);
    }
}
