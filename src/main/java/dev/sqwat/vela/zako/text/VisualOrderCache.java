// Adapted from ZakoOpt 1.0.3, Copyright (C) 2026 Zako. LGPL-3.0-only.
package dev.sqwat.vela.zako.text;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Immutable styled spans keep mutable component trees out of hash-map keys. */
public final class VisualOrderCache {
    private record Span(String text,Style style){}
    private final LruMap<List<Span>,FormattedCharSequence> entries=new LruMap<>(1024);
    private static List<Span> key(FormattedText text){
        var spans=new ArrayList<Span>();
        text.visit((style,value)->{spans.add(new Span(value,style));return Optional.empty();},Style.EMPTY);
        return spans;
    }
    public synchronized FormattedCharSequence get(FormattedText text){return entries.get(key(text));}
    public synchronized void put(FormattedText text,FormattedCharSequence result){entries.put(key(text),result);}
    public synchronized void clear(){entries.clear();}
}
