package dev.sqwat.vela;

import dev.sqwat.vela.module.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ServerRulesTest {
    private static final class Entry extends ClientModule{Entry(String id){super(id,id,"",ModuleCategory.UTILITIES);}}

    @Test void matchesProjectDomainsOnly(){
        assertEquals("ReallyWorld",ServerRules.match("mc.reallyworld.ru").name());
        assertEquals("ReallyWorld",ServerRules.match("MC.PlayRW.ru:25565").name());
        assertEquals("HolyWorld",ServerRules.match("mc.holyworld.me.").name());
        assertEquals("FunTime",ServerRules.match("play.funtime.su").name());
        assertNull(ServerRules.match("reallyworld.ru.evil.com"));
        assertNull(ServerRules.match("notreallyworld.ru"));
        assertNull(ServerRules.match("localhost"));
        assertNull(ServerRules.match(null));
    }

    @Test void switchesOffForbiddenModulesAndRestoresThemOnLeave(){
        var registry=new ModuleRegistry();
        var pearl=registry.register(new Entry("click_pearl"));
        var trajectories=registry.register(new Entry("trajectories"));
        var sprint=registry.register(new Entry("autosprint"));
        pearl.setEnabled(true);trajectories.setEnabled(true);sprint.setEnabled(true);
        var off=ServerRules.join("mc.holyworld.ru",registry);
        assertEquals(java.util.List.of("click_pearl","trajectories"),off.stream().sorted().toList());
        assertFalse(pearl.enabled());assertFalse(trajectories.available());assertTrue(sprint.enabled());
        trajectories.setEnabled(true);
        assertFalse(trajectories.enabled(),"a forbidden module cannot be switched back on while connected");
        ServerRules.leave(registry);
        assertTrue(pearl.enabled());assertTrue(trajectories.enabled());assertTrue(trajectories.available());
    }

    @Test void serverRequestSwitchesOffCompetitiveOrListedModulesUntilLeave(){
        var registry=new ModuleRegistry();
        var pearl=registry.register(new Entry("click_pearl"));
        var sprint=registry.register(new Entry("autosprint"));
        var hat=registry.register(new Entry("china_hat"));
        pearl.setEnabled(true);sprint.setEnabled(true);hat.setEnabled(true);
        ServerRules.join("play.example.net",registry);
        assertEquals(java.util.List.of("click_pearl"),ServerRules.serverRequest(java.util.List.of("*","unknown_id"),registry));
        assertEquals(java.util.List.of("china_hat"),ServerRules.serverRequest(java.util.List.of("china_hat"),registry));
        assertFalse(pearl.available());assertFalse(hat.available());assertTrue(sprint.enabled());
        ServerRules.leave(registry);
        assertTrue(pearl.enabled());assertTrue(hat.enabled());assertTrue(hat.available());
    }

    @Test void serverRequestAddsToKnownRules(){
        var registry=new ModuleRegistry();
        var trajectories=registry.register(new Entry("trajectories"));
        var hat=registry.register(new Entry("china_hat"));
        trajectories.setEnabled(true);hat.setEnabled(true);
        ServerRules.join("mc.holyworld.ru",registry);
        ServerRules.serverRequest(java.util.List.of("china_hat"),registry);
        assertEquals("HolyWorld",ServerRules.active().name());
        assertFalse(trajectories.available());assertFalse(hat.available());
        ServerRules.leave(registry);
        assertTrue(trajectories.enabled());assertTrue(hat.enabled());
    }

    @Test void everyForbiddenIdIsARealModule() throws Exception{
        StringBuilder sources=new StringBuilder();
        try(var files=Files.walk(Path.of("src/main/java/dev/sqwat/vela"))){
            for(var file:files.filter(f->f.toString().endsWith(".java")&&!f.endsWith("ServerRules.java")).toList())sources.append(Files.readString(file));
        }
        for(var project:ServerRules.PROJECTS)for(var id:project.forbidden().keySet())
            assertTrue(sources.indexOf("\""+id+"\"")>=0,project.name()+" forbids unknown module "+id);
        for(var id:ServerRules.COMPETITIVE)assertTrue(sources.indexOf("\""+id+"\"")>=0,"\"*\" covers unknown module "+id);
    }
}
