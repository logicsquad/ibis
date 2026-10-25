package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Checker}.
 * 
 * @author paulh
 */
public class CheckerTest extends AbstractTest {
	private static final String CORRECT_TXT = "/correct.txt";

	private static final String INCORRECT_TXT = "/incorrect.txt";

	private static final String CORRECT = "alpha beta";

	private static final String INCORRECT = "gamma epsilon delta";

	// The expectation here is that this is split into 'A' and 'D' and then both should be dropped as single-char words
	private static final String CHAR_RANGE = "A-D";

	// Dropping single-char words should not stop the words after them being checked
	private static final String INCORRECT_AFTER_SINGLE_CHARS = "alpha A's beta A-D gamma epsilon";

	private static Dictionary dictionary;

	@BeforeAll
	public static void init() {
		dictionary = Dictionary.builder().addWord("alpha").addWord("beta").addWord("gamma").addWord("delta").build();
		return;
	}

	@Test
	public void checkSpellingReturnsEmptyListIfAllCorrect() {
		Checker checker = new Checker(dictionary);
		List<Misspelling> result = checker.checkSpelling(CORRECT);
		assertTrue(result.isEmpty());
		return;
	}

	@Test
	public void checkSpellingReturnsListOfIncorrectWords() {
		Checker checker = new Checker(dictionary);
		List<Misspelling> result = checker.checkSpelling(INCORRECT);
		assertEquals(1, result.size());
		assertEquals("epsilon", result.getFirst().word().text());
		return;
	}

	@Test
	public void singleCharsShouldBeDroppedEvenFromHyphenatedRange() {
		Checker checker = new Checker(dictionary);
		List<Misspelling> result = checker.checkSpelling(CHAR_RANGE);
		assertTrue(result.isEmpty());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/7
	@Test
	public void checkSpellingContinuesAfterDroppedSingleChars() {
		Checker checker = new Checker(dictionary);
		List<Misspelling> result = checker.checkSpelling(INCORRECT_AFTER_SINGLE_CHARS);
		assertEquals(List.of("epsilon"), result.stream().map(m -> m.word().text()).toList());
		return;
	}

	@Test
	public void knownCorrectTextReturnsNoIncorrectWords() {
		Dictionary builtIn = Dictionary.builder(Locale.ENGLISH).addWords().build();
		Checker checker = new Checker(builtIn);
		String text = stringFromResource(CORRECT_TXT);
		assertTrue(checker.checkSpelling(text).isEmpty());
		return;
	}

	@Test
	public void knownIncorrectTextReturnsIncorrectWords() {
		Dictionary builtIn = Dictionary.builder(Locale.ENGLISH).addWords().build();
		Checker checker = new Checker(builtIn);
		String text = stringFromResource(INCORRECT_TXT);
		assertEquals(5, checker.checkSpelling(text).size());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/17
	@Test
	public void checkSpellingUsesGivenTokenizer() {
		Checker checker = new Checker(dictionary, text -> Stream.of(text.split(" ")).map(t -> new Word(t, 0)));
		List<Misspelling> result = checker.checkSpelling("alpha A-D epsilon");
		assertEquals(List.of("A-D", "epsilon"), result.stream().map(m -> m.word().text()).toList());
		return;
	}
}
