package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CheckerTest {
	private static final Dictionary DICTIONARY = new Dictionary();

	private static final String CORRECT = "alpha beta";

	private static final String INCORRECT = "gamma epsilon delta";

	@BeforeAll
	public static void init() {
		DICTIONARY.addWord("alpha");
		DICTIONARY.addWord("beta");
		DICTIONARY.addWord("gamma");
		DICTIONARY.addWord("delta");
		return;
	}

	@Test
	public void checkSpellingReturnsEmptyListIfAllCorrect() {
		Checker checker = new Checker(DICTIONARY);
		List<Word> result = checker.checkSpelling(new Tokenizer(CORRECT));
		assertTrue(result.isEmpty());
		return;
	}

	@Test
	public void checkSpellingReturnsListOfIncorrectWords() {
		Checker checker = new Checker(DICTIONARY);
		List<Word> result = checker.checkSpelling(new Tokenizer(INCORRECT));
		assertEquals(1, result.size());
		assertEquals("epsilon", result.get(0).text());
		return;
	}
}
