package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.*;

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

	@Test
	public void dictionaryCanLoadBuiltInLists() {
		Dictionary d = Dictionary.builder().addWords().build();
		System.out.println("DictionaryTest.dictionaryCanLoadBuiltInLists: d.size() = " + d.size());
	}

	@Test
	public void removeAnnotationThrowsOnNull() {
		assertThrows(NullPointerException.class, () -> Dictionary.Builder.removeAnnotation(null));
		return;
	}

	@Test
	public void removeAnnotationRemovesAnnotations() {
		assertEquals("foo", Dictionary.Builder.removeAnnotation("foo:"));
		assertEquals("bar", Dictionary.Builder.removeAnnotation("bar$"));
		assertEquals("baz", Dictionary.Builder.removeAnnotation("baz+"));
		assertEquals("qux", Dictionary.Builder.removeAnnotation("qux!"));
		assertEquals("quux", Dictionary.Builder.removeAnnotation("quux~"));
		assertEquals("quuux", Dictionary.Builder.removeAnnotation("quuux="));
		assertEquals("quuux", Dictionary.Builder.removeAnnotation("quuux=!~"));
		return;
	}

	@Test
	public void removeAnnotationLeavesNonAnnotatedWordsAlone() {
		assertEquals("foo", Dictionary.Builder.removeAnnotation("foo"));
		assertEquals("bar", Dictionary.Builder.removeAnnotation("bar"));
		assertEquals("e.g.", Dictionary.Builder.removeAnnotation("e.g."));
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
}
