package dev.sqwat.vela;

import dev.sqwat.vela.zako.text.PreparedTextCache;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

class PreparedTextLookupTest {
    private static final Font.PreparedText VALUE=new Font.PreparedText(){
        public void visit(Font.GlyphVisitor visitor){}
        public net.minecraft.client.gui.navigation.ScreenRectangle bounds(){return null;}
    };
    @AfterEach void clear(){PreparedTextCache.clear();}
    @Test void probeMatchesImmutableKeysWithoutFreezingAnimatedText(){
        var text=FormattedCharSequence.forward("test",Style.EMPTY);
        for(float x:new float[]{0,-0f,1,Float.NaN}){
            var key=PreparedTextCache.key(null,text,x,2,3,true,false,4);
            PreparedTextCache.put(key,VALUE);
            assertSame(VALUE,PreparedTextCache.get(null,text,x,2,3,true,false,4));
            assertNull(PreparedTextCache.get(null,text,x,2,3,false,false,4));
            assertNull(PreparedTextCache.get(null,text,x,2,3,true,true,4));
            assertNull(PreparedTextCache.get(null,text,x,2,5,true,false,4));
            assertNull(PreparedTextCache.get(null,text,x,2,3,true,false,5));
        }
        var animated=FormattedCharSequence.forward("test",Style.EMPTY.withObfuscated(true));
        PreparedTextCache.put(PreparedTextCache.key(null,animated,0,0,0,false,false,0),VALUE);
        assertNull(PreparedTextCache.get(null,animated,0,0,0,false,false,0));
        PreparedTextCache.clear();
        assertNull(PreparedTextCache.get(null,text,0,2,3,true,false,4));
    }
    @Test void cacheStaysBoundedAndUsesIdentity(){
        var first=FormattedCharSequence.forward("same",Style.EMPTY);
        PreparedTextCache.put(PreparedTextCache.key(null,first,0,0,0,false,false,0),VALUE);
        assertNull(PreparedTextCache.get(null,FormattedCharSequence.forward("same",Style.EMPTY),0,0,0,false,false,0));
        for(int i=0;i<2048;i++)PreparedTextCache.put(PreparedTextCache.key(null,first,i+1,0,0,false,false,0),VALUE);
        assertNull(PreparedTextCache.get(null,first,0,0,0,false,false,0));
        assertSame(VALUE,PreparedTextCache.get(null,first,2048,0,0,false,false,0));
    }
    @Test void measureLookupAllocations(){
        var bean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        org.junit.jupiter.api.Assumptions.assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        var text=FormattedCharSequence.forward("benchmark",Style.EMPTY);
        PreparedTextCache.put(PreparedTextCache.key(null,text,0,0,0,false,false,0),VALUE);
        for(int i=0;i<100000;i++){PreparedTextCache.get(PreparedTextCache.key(null,text,0,0,0,false,false,0));PreparedTextCache.get(null,text,0,0,0,false,false,0);}
        long id=Thread.currentThread().threadId(),start=bean.getThreadAllocatedBytes(id);
        for(int i=0;i<100000;i++)assertSame(VALUE,PreparedTextCache.get(PreparedTextCache.key(null,text,0,0,0,false,false,0)));
        long oldBytes=bean.getThreadAllocatedBytes(id)-start;start=bean.getThreadAllocatedBytes(id);
        for(int i=0;i<100000;i++)assertSame(VALUE,PreparedTextCache.get(null,text,0,0,0,false,false,0));
        long newBytes=bean.getThreadAllocatedBytes(id)-start;
        System.out.println("100000 cache hits: oldAllocatedBytes="+oldBytes+", newAllocatedBytes="+newBytes);
        assertTrue(newBytes<=oldBytes,"Lookup must not increase allocations");
    }
}
