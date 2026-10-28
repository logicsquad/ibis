package net.logicsquad.ibis.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Lines}.
 *
 * @author paulh
 */
public class LinesTest {
	@Test
	public void linesAndColumnsCountFromOne() {
		Lines lines = new Lines("ab\ncd");
		assertEquals(1, lines.line(0));
		assertEquals(1, lines.column(0));
		assertEquals(2, lines.column(1));
		assertEquals(2, lines.line(3));
		assertEquals(1, lines.column(3));
		assertEquals(2, lines.column(4));
		return;
	}

	@Test
	public void lineEndsAtLineFeedCarriageReturnOrBoth() {
		// a, \r\n, b, \r, c, \n, d
		Lines lines = new Lines("a\r\nb\rc\nd");
		assertEquals(1, lines.line(2));
		assertEquals(2, lines.line(3));
		assertEquals(1, lines.column(3));
		assertEquals(3, lines.line(5));
		assertEquals(4, lines.line(7));
		return;
	}

	@Test
	public void tabAdvancesToNextOfEveryEightColumns() {
		Lines lines = new Lines("\tx\nab\tx\n12345678\tx");
		assertEquals(9, lines.column(1));
		assertEquals(9, lines.column(6));
		assertEquals(17, lines.column(17));
		return;
	}

	@Test
	public void surrogatePairIsOneColumn() {
		Lines lines = new Lines("😀 x");
		assertEquals(3, lines.column(3));
		return;
	}
}
