package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link Checker}.
 * 
 * @author paulh
 */
public class CheckerTest {
	private static Dictionary dictionary;

	private static final String CORRECT = "alpha beta";

	private static final String INCORRECT = "gamma epsilon delta";

	// The expectation here is that this is split into 'A' and 'D' and then both should be dropped as single-char words
	private static final String CHAR_RANGE = "A-D";

	@BeforeAll
	public static void init() {
		dictionary = Dictionary.builder().addWord("alpha").addWord("beta").addWord("gamma").addWord("delta").build();
		return;
	}

	@Test
	public void checkSpellingReturnsEmptyListIfAllCorrect() {
		Checker checker = new Checker(dictionary);
		List<Word> result = checker.checkSpelling(new Tokenizer(CORRECT));
		assertTrue(result.isEmpty());
		return;
	}

	@Test
	public void checkSpellingReturnsListOfIncorrectWords() {
		Checker checker = new Checker(dictionary);
		List<Word> result = checker.checkSpelling(new Tokenizer(INCORRECT));
		assertEquals(1, result.size());
		assertEquals("epsilon", result.getFirst().text());
		return;
	}

	@Test
	public void singleCharsShouldBeDroppedEvenFromHyphenatedRange() {
		Checker checker = new Checker(dictionary);
		List<Word> result = checker.checkSpelling(new Tokenizer(CHAR_RANGE));
		assertTrue(result.isEmpty());
		return;
	}
}
