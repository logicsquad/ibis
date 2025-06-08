package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedList;
import java.util.Queue;

import org.junit.jupiter.api.Test;

public class SpecialCaseHandlerTest {
	private static final String TEXT = " e.g. ";

	private static final Word WORD = Word.of("e.g", 1);

	private static final Word EXPECTED = Word.of("e.g.", 1);

	@Test
	public void handleReturnsExpectedWordForEg() {
		SpecialCaseHandler handler = new SpecialCaseHandler();
		Word result = handler.handle(WORD, TEXT, new LinkedList<>());
		assertEquals(EXPECTED, result);
		return;
	}
}
