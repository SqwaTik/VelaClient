package dev.sqwat.vela.module.utility;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ChatMarkupTest {
    private static List<String> commands(String text){return ChatMarkup.coordinates(text).stream().map(ChatMarkup.Span::command).toList();}
    @Test void findsCoordinateForms(){
        assertEquals(List.of("/vela mark 120 64 -300"),commands("база на 120 64 -300 приходи"));
        assertEquals(List.of("/vela mark 120 64 -300"),commands("база: 120, 64, -300"));
        assertEquals(List.of("/vela mark 10 70 20"),commands("X: 10 Y: 70 Z: 20"));
        assertEquals(List.of("/vela mark -1500 12 800"),commands("x=-1500 y=12 z=800"));
    }
    @Test void ignoresNumbersThatAreNotCoordinates(){
        assertTrue(commands("у меня 3 4 5 яблок").isEmpty());
        assertTrue(commands("в 12:30 встречаемся 15 20").isEmpty());
        assertTrue(commands("100 999 100").isEmpty()); // y out of range
        assertTrue(commands("скидка 50 60 70%").isEmpty());
    }
    @Test void mentionsOnlyAfterTheSender(){
        assertEquals(1,ChatMarkup.mentions("[Игрок] Steve » привет SqwaT09, как дела","SqwaT09").size());
        assertTrue(ChatMarkup.mentions("[Игрок] SqwaT09 » всем привет","SqwaT09").isEmpty());
        assertTrue(ChatMarkup.mentions("SqwaT09 зашёл на сервер","SqwaT09").isEmpty());
        assertTrue(ChatMarkup.mentions("Steve: SqwaT09x не я","SqwaT09").isEmpty());
        assertEquals(1,ChatMarkup.mentions("Steve: @sqwat09 го","SqwaT09").size());
    }
    @Test void privateMessageFormats(){
        assertEquals(new ChatMarkup.Private("Steve",true),ChatMarkup.privateMessage("[Steve -> Я] привет"));
        assertEquals(new ChatMarkup.Private("Steve",true),ChatMarkup.privateMessage("[ЛС] [VIP] Steve » вам: ку"));
        assertEquals(new ChatMarkup.Private("Steve",false),ChatMarkup.privateMessage("[Я -> Steve] привет"));
        assertEquals(new ChatMarkup.Private("Alex",true),ChatMarkup.privateMessage("Alex whispers to you: hi"));
        assertEquals(new ChatMarkup.Private("Alex",false),ChatMarkup.privateMessage("You whisper to Alex: hi"));
        assertNull(ChatMarkup.privateMessage("[Игрок] Steve » привет всем"));
    }
    @Test void applyKeepsTextAndAddsClick(){
        var out=ChatMarkup.apply(Component.literal("иди на 100 64 200"),ChatMarkup.coordinates("иди на 100 64 200"),List.of(),0xFFD166);
        assertEquals("иди на 100 64 200",out.getString());
        var hit=new boolean[1];
        out.visit((style,text)->{if(text.equals("100 64 200")&&style.getClickEvent() instanceof ClickEvent.RunCommand)hit[0]=true;return java.util.Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        assertTrue(hit[0]);
    }
}
