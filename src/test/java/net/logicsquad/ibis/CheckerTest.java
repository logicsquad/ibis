package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

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

	private static Dictionary dictionary;

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

	@Test
	public void knownCorrectTextReturnsNoIncorrectWords() {
		Dictionary builtIn = Dictionary.builder().addWords().build();
		Checker checker = new Checker(builtIn);
		String text = stringFromResource(CORRECT_TXT);
		assertTrue(checker.checkSpelling(new Tokenizer(text)).isEmpty());
		return;
	}

	@Test
	public void knownIncorrectTextReturnsIncorrectWords() {
		Dictionary builtIn = Dictionary.builder().addWords().build();
		Checker checker = new Checker(builtIn);
		String text = stringFromResource(INCORRECT_TXT);
		assertEquals(5, checker.checkSpelling(new Tokenizer(text)).size());
		return;
	}
}
