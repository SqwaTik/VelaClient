package dev.sqwat.vela.ui;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

/** Release discipline: a version bump must come with release notes, and the repository changelog must match. */
class ChangelogTest {
    @Test void newestEntryIsTheModVersion()throws Exception{
        var versions=Changelog.load();
        assertFalse(versions.isEmpty());
        String version=Files.readAllLines(Path.of("gradle.properties")).stream().filter(l->l.startsWith("mod_version=")).findFirst().orElseThrow().substring(12).strip();
        assertEquals(version,versions.getFirst().version(),"Add release notes for "+version+" to changelog.json");
    }
    @Test void everyEntryHasBothLanguages(){
        for(var v:Changelog.load()){
            assertFalse(v.titleEn().isBlank());assertFalse(v.titleRu().isBlank());
            for(var s:v.sections()){assertEquals(s.en().size(),s.ru().size());assertFalse(s.en().isEmpty());}
        }
    }
    @Test void markdownIsUpToDate()throws Exception{
        String md=Files.readString(Path.of("CHANGELOG.md"));
        for(var v:Changelog.load())assertTrue(md.contains("## "+v.version()+" "),"Run tools/changelog_md.py ("+v.version()+" missing)");
    }
}
