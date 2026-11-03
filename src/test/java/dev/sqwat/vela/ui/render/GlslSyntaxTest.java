package dev.sqwat.vela.ui.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlslSyntaxTest {
	private static List<GlslSyntax.Kind> kinds(GlslSyntax.Line line) {
		return line.spans().stream().map(GlslSyntax.Span::kind).toList();
	}

	@Test
	void colorsWordsNumbersAndComments() {
		var line = GlslSyntax.line("vec3 c = mix(a, 1.5e-2, iTime); // note", false);
		assertEquals(List.of(GlslSyntax.Kind.TYPE, GlslSyntax.Kind.PLAIN, GlslSyntax.Kind.PUNCTUATION,
				GlslSyntax.Kind.BUILTIN, GlslSyntax.Kind.PUNCTUATION, GlslSyntax.Kind.PLAIN, GlslSyntax.Kind.PUNCTUATION,
				GlslSyntax.Kind.NUMBER, GlslSyntax.Kind.PUNCTUATION, GlslSyntax.Kind.EDITOR, GlslSyntax.Kind.PUNCTUATION,
				GlslSyntax.Kind.COMMENT), kinds(line));
		assertFalse(line.inComment());
	}

	@Test
	void blockCommentCarriesAcrossLines() {
		var first = GlslSyntax.line("float a; /* start", false);
		assertTrue(first.inComment());
		var second = GlslSyntax.line("still */ return", true);
		assertEquals(List.of(GlslSyntax.Kind.COMMENT, GlslSyntax.Kind.KEYWORD), kinds(second));
		assertFalse(second.inComment());
	}

	@Test
	void preprocessorLine() {
		assertEquals(List.of(GlslSyntax.Kind.PREPROCESSOR, GlslSyntax.Kind.COMMENT),
				kinds(GlslSyntax.line("  #define K 2 // two", false)));
	}

	@Test
	void errorLinesFromDriverLogs() {
		var nvidia = GlslSyntax.errorLines("0(12) : error C0000: syntax error", 20);
		assertTrue(nvidia.get(11));
		var mesa = GlslSyntax.errorLines("0:3(5): error: undeclared\nERROR: 0:7: bad", 20);
		assertTrue(mesa.get(2));
		assertTrue(mesa.get(6));
		assertEquals(0, GlslSyntax.errorLines("1.0:5 something 0(99)", 20).cardinality());
	}
}
