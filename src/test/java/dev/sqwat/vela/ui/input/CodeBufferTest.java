package dev.sqwat.vela.ui.input;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeBufferTest {
	private final AtomicLong now = new AtomicLong(10_000);
	private final CodeBuffer buffer = new CodeBuffer(1000, now::get);

	private void typeAll(String text) {
		text.codePoints().forEach(buffer::type);
	}

	@Test
	void normalizesLineEndingsAndTabs() {
		buffer.set("a\r\n\tb");
		assertEquals("a\n    b", buffer.text());
		assertEquals(2, buffer.lineCount());
		assertEquals("    b", buffer.line(1));
	}

	@Test
	void newlineAfterBraceIndentsAndSplitsBlock() {
		buffer.set("void f(){}");
		buffer.move(9, false);
		buffer.newline();
		assertEquals("void f(){\n    \n}", buffer.text());
		assertEquals(1, buffer.caretLine());
		assertEquals(4, buffer.caretColumn());
	}

	@Test
	void closingBraceDedents() {
		buffer.set("{\n    ");
		buffer.move(buffer.text().length(), false);
		buffer.type('}');
		assertEquals("{\n}", buffer.text());
	}

	@Test
	void backspaceInIndentRemovesToPreviousStop() {
		buffer.set("        x");
		buffer.move(8, false);
		buffer.backspace(false);
		assertEquals("    x", buffer.text());
	}

	@Test
	void tabIndentsAndOutdentsSelectedLines() {
		buffer.set("a\nb");
		buffer.selectAll();
		buffer.tab(false);
		assertEquals("    a\n    b", buffer.text());
		buffer.selectAll();
		buffer.tab(true);
		assertEquals("a\nb", buffer.text());
	}

	@Test
	void verticalKeepsPreferredColumn() {
		buffer.set("abcdef\nab\nabcdef");
		buffer.move(5, false);
		buffer.vertical(1, false);
		assertEquals(2, buffer.caretColumn());
		buffer.vertical(1, false);
		assertEquals(5, buffer.caretColumn());
	}

	@Test
	void typingMergesIntoOneUndoStep() {
		buffer.set("");
		typeAll("abc");
		now.addAndGet(5000);
		typeAll("d");
		assertTrue(buffer.undo());
		assertEquals("abc", buffer.text());
		assertTrue(buffer.undo());
		assertEquals("", buffer.text());
		assertFalse(buffer.undo());
		assertTrue(buffer.redo());
		assertEquals("abc", buffer.text());
	}

	@Test
	void toggleCommentRoundTrips() {
		buffer.set("    a\n    b");
		buffer.selectAll();
		buffer.toggleComment();
		assertTrue(buffer.line(0).contains("// a"));
		assertTrue(buffer.line(1).contains("// b"));
		buffer.selectAll();
		buffer.toggleComment();
		assertEquals("    a\n    b", buffer.text());
	}

	@Test
	void insertTruncatesToLimit() {
		var small = new CodeBuffer(4, now::get);
		small.set("ab");
		small.move(2, false);
		assertTrue(small.insert("xyz"));
		assertEquals("abxy", small.text());
		assertFalse(small.insert("z"));
	}

	@Test
	void duplicateLinesCopiesCurrentLine() {
		buffer.set("a\nb");
		buffer.move(0, false);
		buffer.duplicateLines();
		assertEquals("a\na\nb", buffer.text());
	}
}
