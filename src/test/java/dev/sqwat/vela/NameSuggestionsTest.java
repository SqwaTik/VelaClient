package dev.sqwat.vela;
import dev.sqwat.vela.friend.NameSuggestions;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NameSuggestionsTest {
    @Test void matchesPrefixWithoutRankAndLimitsResults(){
        assertEquals(List.of("MrHaryus","MrOther"),NameSuggestions.matching(List.of("MrOther","MrHaryus","OtherMr","[VIP] MrBot","Mr"),"mr"));
        assertTrue(NameSuggestions.matching(List.of("MrHaryus"),"").isEmpty());
        assertTrue(NameSuggestions.matching(List.of("MrHaryus"),"MRHARYUS").isEmpty());
        assertEquals(5,NameSuggestions.matching(List.of("MrOne","MrTwo","MrThree","MrFour","MrFive","MrSix"),"Mr").size());
    }
}
