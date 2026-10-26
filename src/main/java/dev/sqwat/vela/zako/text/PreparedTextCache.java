// Adapted from ZakoOpt 1.1.6, Copyright (C) 2026 Zako. LGPL-3.0-only.
// See META-INF/licenses/zakoopt/NOTICE and CHANGES.md.
package dev.sqwat.vela.zako.text;

import dev.sqwat.vela.zako.feature.Features;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@UtilityClass
public class PreparedTextCache {
	private final Font.PreparedText UNCACHEABLE = new Font.PreparedText() {
		@Override
		public void visit(Font.GlyphVisitor visitor) {
		}

		@Override
		public net.minecraft.client.gui.navigation.ScreenRectangle bounds() {
			return null;
		}
	};

	private final Map<Key, Font.PreparedText> CACHE = new LinkedHashMap<>(512, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Key, Font.PreparedText> eldest) {
			return size() > 2048;
		}
	};

	public boolean isStable(FormattedCharSequence text) {
		return StableText.isStable(text);
	}

	public boolean enabled() {
		return Features.PREPARED_TEXT_CACHE.on;
	}

	public Key key(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background) {
		return new Key(font, text, x, y, color, shadow, includeEmpty, background);
	}

	public Font.PreparedText get(Key key) {
		synchronized (CACHE) {
			Font.PreparedText cached = CACHE.get(key);
			return cached == UNCACHEABLE ? null : cached;
		}
	}

	// Only used under CACHE's monitor; never stored in the map or retained across calls.
	private final Lookup LOOKUP = new Lookup();
	public Font.PreparedText get(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background) {
		synchronized (CACHE) {
			LOOKUP.font=font;LOOKUP.text=text;LOOKUP.x=x;LOOKUP.y=y;
			LOOKUP.color=color;LOOKUP.shadow=shadow;LOOKUP.includeEmpty=includeEmpty;LOOKUP.background=background;
			try {
				var cached=CACHE.get(LOOKUP);
				return cached==UNCACHEABLE?null:cached;
			} finally { LOOKUP.font=null;LOOKUP.text=null; }
		}
	}
	private static final class Lookup {
		Font font;FormattedCharSequence text;float x,y;int color,background;boolean shadow,includeEmpty;
		@Override public boolean equals(Object other){
			return other instanceof Key k&&k.font==font&&k.text==text
				&&Float.floatToIntBits(k.x)==Float.floatToIntBits(x)&&Float.floatToIntBits(k.y)==Float.floatToIntBits(y)
				&&k.color==color&&k.background==background&&k.shadow==shadow&&k.includeEmpty==includeEmpty;
		}
		@Override public int hashCode(){return hash(font,text,x,y,color,shadow,includeEmpty,background);}
	}
	private int hash(Font font,FormattedCharSequence text,float x,float y,int color,boolean shadow,boolean includeEmpty,int background){
		int h=31*System.identityHashCode(font)+System.identityHashCode(text);
		h=31*h+Float.floatToIntBits(x);h=31*h+Float.floatToIntBits(y);
		h=31*h+color;h=31*h+background;
		return 31*h+(shadow?1:0)+(includeEmpty?2:0);
	}

	public boolean known(Key key) {
		synchronized (CACHE) {
			return CACHE.containsKey(key);
		}
	}

	public void put(Key key, Font.PreparedText prepared) {
		// obfuscated (§k) glyphs are re-randomized on every prepare, so they must not be frozen
		boolean plain = key.text.accept((index, style, codePoint) -> !style.isObfuscated());
		synchronized (CACHE) {
			CACHE.put(key, plain ? prepared : UNCACHEABLE);
		}
	}

	public void clear() {
		synchronized (CACHE) {
			CACHE.clear();
		}
	}

	public record Key(Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow, boolean includeEmpty, int background) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Key k && k.font == font && k.text == text && Float.floatToIntBits(k.x) == Float.floatToIntBits(x) && Float.floatToIntBits(k.y) == Float.floatToIntBits(y) && k.color == color
					&& k.shadow == shadow && k.includeEmpty == includeEmpty && k.background == background;
		}

		@Override
		public int hashCode() {
			return hash(font,text,x,y,color,shadow,includeEmpty,background);
		}
	}
}
