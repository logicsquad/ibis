package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
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
		assertThrows(NullPointerException.class, () -> Word.of(null, 0));
		return;
	}

	@Test
	public void startCannotBeNegative() {
		assertThrows(IllegalArgumentException.class, () -> Word.of("foo", -1));
		return;
	}

	@Test
	public void toLowerCaseReturnsExpectedResult() {
		assertEquals("expected", Word.of("expected", 0).toLowerCase());
		assertEquals("expected", Word.of("Expected", 0).toLowerCase());
		assertEquals("expected", Word.of("EXPECTED", 0).toLowerCase());
		assertEquals("expected", Word.of("eXPECted", 0).toLowerCase());
		assertEquals("expected", Word.of("expecteD", 0).toLowerCase());
		return;
	}

	@Test
	public void toInitialCapReturnsExpectedResult() {
		assertEquals("Expected", Word.of("expected", 0).toInitialCap());
		assertEquals("Expected", Word.of("Expected", 0).toInitialCap());
		assertEquals("Expected", Word.of("EXPECTED", 0).toInitialCap());
		assertEquals("Expected", Word.of("eXPECted", 0).toInitialCap());
		assertEquals("Expected", Word.of("expecteD", 0).toInitialCap());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/8
	@Test
	public void toInitialCapReturnsEmptyStringForEmptyText() {
		assertEquals("", Word.of("", 0).toInitialCap());
		return;
	}

	// In Turkish, "I" lowercases to dotless "ı", and "i" uppercases to dotted "İ"
	// https://github.com/logicsquad/ibis/issues/10
	@Test
	public void caseConversionIgnoresDefaultLocale() {
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr"));
		try {
			assertEquals("inside", Word.of("INSIDE", 0).toLowerCase());
			assertEquals("Inside", Word.of("INSIDE", 0).toInitialCap());
			assertEquals("Inside", Word.of("inside", 0).toInitialCap());
		} finally {
			Locale.setDefault(defaultLocale);
		}
		return;
	}

	// This is merely to cover a bug after introducing the call to Collections.unmodifiableList()
	@Test
	public void suggestionsReturnsNullIfSuggestionsIsNull() {
		Word w = Word.of("foo", 0);
		// We know w.suggestions is null
		assertDoesNotThrow(() -> w.suggestions());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/12
	@Test
	public void suggestionsAreCopiedOnConstruction() {
		List<String> suggestions = new ArrayList<>(List.of("one"));
		Word word = Word.of("x", 0, suggestions);
		Word copy = Word.of("x", 0).withSuggestions(suggestions);
		suggestions.add("two");
		assertEquals(List.of("one"), word.suggestions());
		assertEquals(List.of("one"), copy.suggestions());
		return;
	}
}
