package dev.sqwat.vela;
import dev.sqwat.vela.render.NameMask;
import dev.sqwat.vela.module.setting.TextSetting;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NameMaskTest {
    @org.junit.jupiter.api.io.TempDir java.nio.file.Path root;
    private static class TestModule extends dev.sqwat.vela.module.ClientModule {
        final TextSetting alias=setting(new TextSetting("alias","Alias","","Player"));
        TestModule(){super("streamer_test","Test","",dev.sqwat.vela.module.ModuleCategory.UTILITIES);}
    }
    @Test void aliasPersistsInNormalProfiles()throws Exception{
        var registry=new dev.sqwat.vela.module.ModuleRegistry();var first=new TestModule();registry.register(first);
        try(var configs=new dev.sqwat.vela.config.ConfigService(root,registry)){first.alias.set("Live Guest");configs.flush();}
        var other=new dev.sqwat.vela.module.ModuleRegistry();var second=new TestModule();other.register(second);
        try(var configs=new dev.sqwat.vela.config.ConfigService(root,other)){assertEquals("Live Guest",second.alias.value());}
    }
    @Test void exactNamesNotOtherPlayers(){
        var mask=new NameMask("Alice","Guest");
        assertEquals("<Guest> hi Guest!",mask.plain("<Alice> hi ALICE!"));
        assertEquals("Alice2 _Alice Alice_",mask.plain("Alice2 _Alice Alice_"));
        assertEquals("unchanged",mask.plain("unchanged"));
        assertEquals("anything",new NameMask("","Guest").plain("anything"));
    }
    @Test void styledNameAcrossSegmentsIsItalicAndOriginalUnchanged(){
        var original=Component.literal("[VIP] ").append(Component.literal("Al").withStyle(s->s.withColor(0x123456)))
            .append(Component.literal("ice")).append(" hello");
        var result=new NameMask("Alice","Guest").formatted(original);
        assertEquals("[VIP] Alice hello",original.getString());assertEquals("[VIP] Guest hello",result.getString());
        StringBuilder italic=new StringBuilder();
        result.visit((style,text)->{if(style.isItalic())italic.append(text);return java.util.Optional.empty();},Style.EMPTY);
        assertEquals("Guest",italic.toString());
    }
    @Test void sequenceIsIdempotentEvenWhenAliasContainsOriginal(){
        var mask=new NameMask("Alice","Alice TV");
        var input=FormattedCharSequence.forward("Alice",Style.EMPTY);
        var output=mask.sequence(input);assertSame(output,mask.sequence(output));
        assertSame(output,mask.sequence(input));
        var formatted=mask.formatted(Component.literal("Alice"));
        assertEquals("Alice TV",formatted.getString());assertSame(formatted,mask.formatted(formatted));
    }
    @Test void aliasRejectsControlsFormattingAndEmptyValues(){
        var text=new TextSetting("alias","Alias","","Player");text.set("  \n§\u202E  ");assertEquals("Player",text.value());
        text.set("a".repeat(50));assertEquals(24,text.value().length());
    }
    @Test void nativeFontTargetsExist()throws Exception{
        try(var in=getClass().getResourceAsStream("/net/minecraft/client/gui/Font.class")){
            var node=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(in).accept(node,org.objectweb.asm.ClassReader.SKIP_CODE);
            for(String signature:java.util.List.of("prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;","prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;","width(Lnet/minecraft/util/FormattedCharSequence;)I"))
                assertTrue(node.methods.stream().anyMatch(m->(m.name+m.desc).equals(signature)),signature);
        }
    }
}
