package dev.sqwat.vela.render.entity;

import java.util.LinkedHashMap;

/** Render-thread-only bounded cache; stored text keys must be immutable. */
public final class PreparedLabelCache<T> {
    public record Key(Object font, Object text, float x, float y, int color, int background) {
        /** A mutable key is safe only for a lookup; cache entries always use this immutable record. */
        public static Lookup lookup() { return new Lookup(); }
        @Override public boolean equals(Object other) {
            return other instanceof Key k && font == k.font && text.equals(k.text)
                && Float.floatToIntBits(x) == Float.floatToIntBits(k.x)
                && Float.floatToIntBits(y) == Float.floatToIntBits(k.y)
                && color == k.color && background == k.background;
        }
        @Override public int hashCode() {
            int h = 31 * System.identityHashCode(font) + text.hashCode();
            h = 31 * h + Float.floatToIntBits(x);
            h = 31 * h + Float.floatToIntBits(y);
            return 31 * (31 * h + color) + background;
        }
    }
    public static final class Lookup {
        private Object font,text; private float x,y; private int color,background;
        public Lookup set(Object font,Object text,float x,float y,int color,int background){this.font=font;this.text=text;this.x=x;this.y=y;this.color=color;this.background=background;return this;}
        @Override public boolean equals(Object other) {
            return other instanceof Key k && font==k.font() && text.equals(k.text())
                && Float.floatToIntBits(x)==Float.floatToIntBits(k.x()) && Float.floatToIntBits(y)==Float.floatToIntBits(k.y())
                && color==k.color() && background==k.background();
        }
        @Override public int hashCode() {
            int h=31*System.identityHashCode(font)+text.hashCode();h=31*h+Float.floatToIntBits(x);h=31*h+Float.floatToIntBits(y);
            return 31*(31*h+color)+background;
        }
    }
    private final int capacity;
    private final LinkedHashMap<Key, T> entries = new LinkedHashMap<>(16, .75f, true);
    public PreparedLabelCache(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
    }
    public T get(Key key) { return entries.get(key); }
    public T get(Lookup key) { return entries.get(key); }
    public void put(Key key, T value) {
        entries.put(key, value);
        if (entries.size() > capacity) entries.pollFirstEntry();
    }
    public void clear() { entries.clear(); }
}
