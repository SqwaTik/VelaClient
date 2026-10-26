package dev.sqwat.vela;

import dev.sqwat.vela.module.ZakoOptimization;
import dev.sqwat.vela.zako.ZakoOptConfig;
import dev.sqwat.vela.zako.ZakoOptions;
import dev.sqwat.vela.zako.text.StableText;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ZakoDefaultsTest {
    @Test void visualOrderCacheTracksMutatedTextAndStyles(){
        var cache=new dev.sqwat.vela.zako.text.VisualOrderCache();
        var component=net.minecraft.network.chat.Component.literal("Name");
        FormattedCharSequence result=sink->true;
        cache.put(component,result);assertSame(result,cache.get(component));
        component.append(" changed");assertNull(cache.get(component));
        assertSame(result,cache.get(net.minecraft.network.chat.Component.literal("Name")));
        component=net.minecraft.network.chat.Component.literal("Name").withStyle(net.minecraft.ChatFormatting.RED);
        assertNull(cache.get(component));cache.clear();assertNull(cache.get(net.minecraft.network.chat.Component.literal("Name")));
    }
    @Test void allUpstreamOptionsRetainTheirDefaults(){
        var expected=ZakoOptions.pages(new ZakoOptConfig.Values()).stream().flatMap(p->p.groups().stream()).flatMap(g->g.options().stream()).toList();
        var actual=ZakoOptions.pages(new ZakoOptimization().values()).stream().flatMap(p->p.groups().stream()).flatMap(g->g.options().stream()).toList();
        assertEquals(expected.size(),actual.size());
        for(int i=0;i<expected.size();i++){
            var a=expected.get(i);var b=actual.get(i);assertEquals(a.key(),b.key());
            if(a instanceof ZakoOptions.Toggle t)assertEquals(t.get().get(),((ZakoOptions.Toggle)b).get().get(),a.key());
            else assertEquals(((ZakoOptions.Distance)a).get().get(),((ZakoOptions.Distance)b).get().get(),a.key());
        }
    }
    @Test void stableTextDoesNotAccumulateWrapperLayers(){
        FormattedCharSequence sequence=sink->true;
        var stable=StableText.mark(sequence);
        for(int n=0;n<10000;n++)assertSame(stable,StableText.mark(stable));
    }
}
