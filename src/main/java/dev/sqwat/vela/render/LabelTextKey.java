package dev.sqwat.vela.render;

import java.util.Arrays;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

/** A reusable lookup probe; only immutable copies may be stored in the cache. */
public final class LabelTextKey implements FormattedCharSink {
    private final int[] points;
    private final Style[] styles;
    private final boolean frozen;
    private int size, hash;
    private Style lastStyle;
    private int lastStyleHash;
    public LabelTextKey() { this(new int[256], new Style[256], 0, 1, false); }
    private LabelTextKey(int[] points, Style[] styles, int size, int hash, boolean frozen) {
        this.points = points; this.styles = styles; this.size = size; this.hash = hash; this.frozen = frozen;
    }
    public boolean read(FormattedCharSequence text) {
        if (frozen) throw new IllegalStateException("Immutable text key");
        Arrays.fill(styles, 0, size, null);
        size = 0; hash = 1; lastStyle=null;
        try {return text.accept(this);} finally {lastStyle=null;}
    }
    @Override public boolean accept(int index, Style style, int point) {
        if (frozen) throw new IllegalStateException("Immutable text key");
        if (size == 256 || style.isObfuscated() || !(style.getFont() instanceof FontDescription.Resource)) return false;
        // Vanilla extends underline/strikethrough at the start of each sequence run.
        int layoutPoint=point|(index==0?1<<21:0);
        points[size] = layoutPoint; styles[size++] = style;
        if(style!=lastStyle){lastStyleHash=style.hashCode();lastStyle=style;}
        hash = 31 * (31 * hash + layoutPoint) + lastStyleHash;
        return true;
    }
    public LabelTextKey snapshot() {
        return new LabelTextKey(Arrays.copyOf(points, size), Arrays.copyOf(styles, size), size, hash, true);
    }
    @Override public int hashCode() { return hash; }
    @Override public boolean equals(Object other) {
        if (!(other instanceof LabelTextKey key) || size != key.size || hash != key.hash) return false;
        for (int i = 0; i < size; i++) {
            if (points[i] != key.points[i] || !styles[i].equals(key.styles[i])) return false;
        }
        return true;
    }
}
