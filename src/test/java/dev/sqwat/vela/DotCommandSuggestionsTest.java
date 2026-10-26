package dev.sqwat.vela;
import dev.sqwat.vela.command.DotCommandSuggestions;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DotCommandSuggestionsTest {
    @Test void offersCommandsImmediately(){assertEquals(List.of(".cfg",".gps"),DotCommandSuggestions.suggest(".",1,List.of()).getList().stream().map(s->s.getText()).toList());}
    @Test void completesOnlyCurrentArgument(){
        var result=DotCommandSuggestions.suggest(".cfg lo",7,List.of());
        assertEquals(".cfg load",result.getList().getFirst().apply(".cfg lo"));
        assertEquals(".cfg load pvp",DotCommandSuggestions.suggest(".cfg load pv",12,List.of("pvp","build")).getList().getFirst().apply(".cfg load pv"));
        assertTrue(DotCommandSuggestions.suggest(".cfg dir ",9,List.of("pvp")).isEmpty());
    }
}
