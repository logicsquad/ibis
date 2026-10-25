package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Dictionary}.
 * 
 * @author paulh
 */
public class DictionaryTest {
	private static Dictionary dictionary;

	@BeforeAll
	public static void setup() {
		dictionary = Dictionary.builder().addWord("alpha").addWord("beta").build();
		return;
	}

	@Test
	public void isCorrectReturnsTrueIfWordPresent() {
		assertTrue(dictionary.isCorrect(Word.of("alpha", 0)));
		assertTrue(dictionary.isCorrect(Word.of("beta", 0)));
		return;
	}

	@Test
	public void isCorrectReturnsFalseIfWordNotPresent() {
		assertFalse(dictionary.isCorrect(Word.of("gamma", 0)));
		assertFalse(dictionary.isCorrect(Word.of("delta", 0)));
		return;
	}

	// The problem here is that "AUSTRALIAN" is being flagged incorrect, even though "Australian" is in the word list.
	@Test
	public void isCorrectShouldTryInitialCaps() {
		Dictionary d = Dictionary.builder().addWord("Australian").build();
		assertTrue(d.isCorrect(Word.of("Australian", 0)));
		assertTrue(d.isCorrect(Word.of("AUSTRALIAN", 0)));
		return;
	}

	// "why" has an empty Metaphone code, so an empty Word is looked up among its neighbours
	// https://github.com/logicsquad/ibis/issues/8
	@Test
	public void isCorrectReturnsFalseForEmptyWord() {
		Dictionary d = Dictionary.builder().addWord("why").build();
		assertFalse(d.isCorrect(Word.of("", 0)));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/9
	@Test
	public void addWordsAddsBuiltInWordsForAnyEnglishLocale() {
		for (Locale locale : List.of(Locale.ENGLISH, Locale.US, Locale.UK, Locale.forLanguageTag("en-AU"))) {
			assertTrue(Dictionary.builder(locale).addWords().build().isCorrect(Word.of("the", 0)), locale.toString());
		}
		return;
	}

	// https://github.com/logicsquad/ibis/issues/9
	@Test
	public void builderWithoutLocaleAddsEnglishBuiltInWords() {
		assertTrue(Dictionary.builder().addWords().build().isCorrect(Word.of("the", 0)));
		return;
	}
}
