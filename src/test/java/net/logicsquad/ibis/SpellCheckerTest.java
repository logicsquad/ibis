package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SpellCheckerTest {
	private static final String CONTAINS_DIGIT_1 = "1";
	private static final String CONTAINS_DIGIT_2 = "1a1";
	private static final String CONTAINS_DIGIT_3 = "131";
	private static final String CONTAINS_DIGIT_4 = "a31";
	private static final String CONTAINS_DIGIT_5 = "2001";
	private static final String CONTAINS_DIGIT_6 = "15T03";
	private static final String CONTAINS_DIGIT_7 = "00Z";

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
		assertTrue(SpellChecker.containsDigit(Word.of(text, 0)));
		return;
	}
}
