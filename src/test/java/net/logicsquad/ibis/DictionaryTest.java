package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Dictionary}.
 * 
 * @author paulh
 */
public class DictionaryTest {
	private static final Dictionary DICTIONARY = new Dictionary();

	@BeforeAll
	public static void setup() {
		DICTIONARY.addWord("alpha");
		DICTIONARY.addWord("beta");
		return;
	}

	@Test
	public void isCorrectReturnsTrueIfWordPresent() {
		assertTrue(DICTIONARY.isCorrect(Word.of("alpha", 0)));
		assertTrue(DICTIONARY.isCorrect(Word.of("beta", 0)));
		return;
	}

	@Test
	public void isCorrectReturnsFalseIfWordNotPresent() {
		assertFalse(DICTIONARY.isCorrect(Word.of("gamma", 0)));
		assertFalse(DICTIONARY.isCorrect(Word.of("delta", 0)));
		return;
	}

	@Test
	public void addWordIsIdempotent() {
		Dictionary dictionary = new Dictionary();
		assertTrue(dictionary.map.isEmpty());
		dictionary.addWord("alpha");
		assertEquals(1, dictionary.map.size());
		var entry = dictionary.map.entrySet().iterator().next();
		assertEquals(1, entry.getValue().size());
		dictionary.addWord("alpha");
		assertEquals(1, dictionary.map.size());
		entry = dictionary.map.entrySet().iterator().next();
		assertEquals(1, entry.getValue().size());
		return;
	}
}
