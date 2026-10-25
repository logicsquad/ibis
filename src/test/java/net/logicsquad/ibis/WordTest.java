package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Word}.
 * 
 * @author paulh
 */
public class WordTest {
	@Test
	public void textCannotBeNull() {
		assertThrows(NullPointerException.class, () -> new Word(null, 0));
		return;
	}

	@Test
	public void startCannotBeNegative() {
		assertThrows(IllegalArgumentException.class, () -> new Word("foo", -1));
		return;
	}

	@Test
	public void toLowerCaseReturnsExpectedResult() {
		assertEquals("expected", new Word("expected", 0).toLowerCase());
		assertEquals("expected", new Word("Expected", 0).toLowerCase());
		assertEquals("expected", new Word("EXPECTED", 0).toLowerCase());
		assertEquals("expected", new Word("eXPECted", 0).toLowerCase());
		assertEquals("expected", new Word("expecteD", 0).toLowerCase());
		return;
	}

	@Test
	public void toInitialCapReturnsExpectedResult() {
		assertEquals("Expected", new Word("expected", 0).toInitialCap());
		assertEquals("Expected", new Word("Expected", 0).toInitialCap());
		assertEquals("Expected", new Word("EXPECTED", 0).toInitialCap());
		assertEquals("Expected", new Word("eXPECted", 0).toInitialCap());
		assertEquals("Expected", new Word("expecteD", 0).toInitialCap());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/8
	@Test
	public void toInitialCapReturnsEmptyStringForEmptyText() {
		assertEquals("", new Word("", 0).toInitialCap());
		return;
	}

	// In Turkish, "I" lowercases to dotless "ı", and "i" uppercases to dotted "İ"
	// https://github.com/logicsquad/ibis/issues/10
	@Test
	public void caseConversionIgnoresDefaultLocale() {
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr"));
		try {
			assertEquals("inside", new Word("INSIDE", 0).toLowerCase());
			assertEquals("Inside", new Word("INSIDE", 0).toInitialCap());
			assertEquals("Inside", new Word("inside", 0).toInitialCap());
		} finally {
			Locale.setDefault(defaultLocale);
		}
		return;
	}
}
