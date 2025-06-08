package net.logicsquad.ibis;

import static net.logicsquad.ibis.Rejector.containsDigit;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RejectorTest {
	private static final String CONTAINS_DIGIT_1 = "1";
	private static final String CONTAINS_DIGIT_2 = "1a1";
	private static final String CONTAINS_DIGIT_3 = "131";
	private static final String CONTAINS_DIGIT_4 = "a31";
	private static final String CONTAINS_DIGIT_5 = "2001";
	private static final String CONTAINS_DIGIT_6 = "15T03";
	private static final String CONTAINS_DIGIT_7 = "00Z";

	private static final String HYPHEN = "-";

	@Test
	public void containsDigitReturnsTrueIfWordContainsDigit() {
		testContainsDigitForString(CONTAINS_DIGIT_1);
		testContainsDigitForString(CONTAINS_DIGIT_2);
		testContainsDigitForString(CONTAINS_DIGIT_3);
		testContainsDigitForString(CONTAINS_DIGIT_4);
		testContainsDigitForString(CONTAINS_DIGIT_5);
		testContainsDigitForString(CONTAINS_DIGIT_6);
		testContainsDigitForString(CONTAINS_DIGIT_7);
		return;
	}

	private void testContainsDigitForString(String text) {
		assertTrue(containsDigit(Word.of(text, 0)));
		return;
	}

	@Test
	public void rejectorRejectsWordsWithDigits() {
		Rejector rejector = new Rejector();
		assertTrue(rejector.reject(Word.of(CONTAINS_DIGIT_1, 0)));
		assertTrue(rejector.reject(Word.of(CONTAINS_DIGIT_2, 0)));
		assertTrue(rejector.reject(Word.of(CONTAINS_DIGIT_3, 0)));
		assertTrue(rejector.reject(Word.of(CONTAINS_DIGIT_4, 0)));
		assertTrue(rejector.reject(Word.of(CONTAINS_DIGIT_5, 0)));
		assertTrue(rejector.reject(Word.of(CONTAINS_DIGIT_6, 0)));
		assertTrue(rejector.reject(Word.of(CONTAINS_DIGIT_7, 0)));
		return;
	}

	@Test
	public void rejectorRejectsSingleHyphen() {
		Rejector rejector = new Rejector();
		assertTrue(rejector.reject(Word.of(HYPHEN, 0)));
		return;
	}
}
