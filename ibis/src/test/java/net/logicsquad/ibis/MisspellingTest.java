package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Misspelling}.
 * @author paulh
 */
public class MisspellingTest {
	@Test
	public void wordCannotBeNull() {
		assertThrows(NullPointerException.class, () -> new Misspelling(null, List.of()));
		return;
	}

	@Test
	public void suggestionsCannotBeNull() {
		assertThrows(NullPointerException.class, () -> new Misspelling(new Word("x", 0), null));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/12
	@Test
	public void suggestionsAreCopiedOnConstruction() {
		List<String> suggestions = new ArrayList<>(List.of("one"));
		Misspelling misspelling = new Misspelling(new Word("x", 0), suggestions);
		suggestions.add("two");
		assertEquals(List.of("one"), misspelling.suggestions());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/16
	@Test
	public void equalityIncludesSuggestions() {
		Word word = new Word("x", 0);
		assertEquals(new Misspelling(word, List.of("one")), new Misspelling(word, List.of("one")));
		assertNotEquals(new Misspelling(word, List.of("one")), new Misspelling(word, List.of("two")));
		return;
	}
}
