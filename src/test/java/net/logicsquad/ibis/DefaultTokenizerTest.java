package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link DefaultTokenizer}.
 * 
 * @author paulh
 */
public class DefaultTokenizerTest {
	private static final String TEST_1 = """
			Now (is the time) for all, good—men to come to the aid of the party.
			""";
	private static final List<String> EXPECTED_1 = List.of("Now", "is", "the", "time", "for", "all", "good", "men", "to", "come", "to", "the", "aid", "of",
			"the", "party");

	private static final String TEST_2 = """
			Sometimes, e.g. and then later i.e. Crazy!
			""";
	private static final List<String> EXPECTED_2 = List.of("Sometimes", "e.g.", "and", "then", "later", "i.e.", "Crazy");

	private static final String TEST_3 = "Now is the time.";
	private static final List<Word> EXPECTED_3 = List.of(Word.of("Now", 0), Word.of("is", 4), Word.of("the", 7), Word.of("time", 11));

	private static final String TEST_4 = "Here is my—em dash";
	private static final List<Word> EXPECTED_4 = List.of(Word.of("Here", 0), Word.of("is", 5), Word.of("my", 8), Word.of("em", 11), Word.of("dash", 14));

	private static final String TEST_5 = "Now e.g. red one.";
	private static final List<Word> EXPECTED_5 = List.of(Word.of("Now", 0), Word.of("e.g.", 4), Word.of("red", 9), Word.of("one", 13));

	private static final String BLANK_1 = "";
	private static final String BLANK_2 = "   ";
	private static final String BLANK_3 = "\n";
	private static final String BLANK_4 = "\t\n ";

	private static final String TEST_6 = "Here—e.g. one—is a tit-for-tat—crazy one–two—example!";
	private static final Word WORD_6A = Word.of("Here", 0);
	private static final Word WORD_6B = Word.of("e.g.", 5);
	private static final Word WORD_6C = Word.of("one", 10);
	private static final Word WORD_6D = Word.of("is", 14);
	private static final Word WORD_6F = Word.of("tit", 19);
	private static final Word WORD_6G = Word.of("for", 23);
	private static final Word WORD_6H = Word.of("tat", 27);
	private static final Word WORD_6I = Word.of("crazy", 31);
	private static final Word WORD_6J = Word.of("one", 37);
	private static final Word WORD_6K = Word.of("two", 41);
	private static final Word WORD_6L = Word.of("example", 45);
	private static final List<Word> EXPECTED_6 = List.of(WORD_6A, WORD_6B, WORD_6C, WORD_6D, WORD_6F, WORD_6G, WORD_6H, WORD_6I, WORD_6J, WORD_6K,
			WORD_6L);

	private static final String TEST_7 = "It looks like the client’s, doesn't it?";
	private static final String COOKED_7 = "It looks like the client's, doesn't it?";
	private static final List<Word> EXPECTED_7 = List.of(Word.of("It", 0), Word.of("looks", 3), Word.of("like", 9), Word.of("the", 14), Word.of("client", 18),
			Word.of("doesn't", 28), Word.of("it", 36));

	private static final String TEST_8 = "“‘we'll’, ‘they‘ll’, ‘don՚t’, ‘can＇t’, ‘weߴre’, ‘wonʼt’";
	private static final List<Word> EXPECTED_8 = List.of(Word.of("we'll", 2), Word.of("they'll", 11), Word.of("don't", 22), Word.of("can't", 31),
			Word.of("we're", 40), Word.of("won't", 49));

	private static final String TEST_9 = "alpha—beta gamma";
	private static final List<Word> EXPECTED_9 = List.of(Word.of("alpha", 0), Word.of("beta", 6), Word.of("gamma", 11));

	private static final String TEST_10 = "one 2022-11-03T02:09:00Z two";
	private static final List<Word> EXPECTED_10 = List.of(Word.of("one", 0), Word.of("two", 25));

	// "E.g." and "I.e." should be handled as special cases even with initial caps
	private static final String TEST_11 = "(E.g. one.) (I.e. another.)";
	private static final List<Word> EXPECTED_11 = List.of(Word.of("E.g.", 1), Word.of("one", 6), Word.of("I.e.", 13), Word.of("another", 18));

	// More trouble with dashes
	private static final String EM_DASH_1 = "CHAPTER 74. The Sperm Whale’s Head—Contrasted View.";
	private static final String EM_DASH_2 = "CHAPTER 105. Does the Whale’s Magnitude Diminish?—Will He Perish?";
	private static final String EM_DASH_3 = "CHAPTER 121. Midnight.—The Forecastle Bulwarks.";
	private static final String EM_DASH_4 = """
			great Leviathan, called a Commonwealth or
  State—(in Latin, Civitas) which is but an artificial man
			""";

	// Words that the Handler reduces to a single character should be dropped without dropping the rest of the text
	private static final String POSSESSIVE_SINGLE_CHAR = "Grade A's are fine.";
	private static final List<Word> EXPECTED_POSSESSIVE_SINGLE_CHAR = List.of(Word.of("Grade", 0), Word.of("are", 10), Word.of("fine", 14));
	private static final String HYPHENATED_SINGLE_CHARS = "We did A-B testing.";
	private static final List<Word> EXPECTED_HYPHENATED_SINGLE_CHARS = List.of(Word.of("We", 0), Word.of("did", 3), Word.of("testing", 11));
	private static final String HYPHENATED_TRAILING_SINGLE_CHAR = "Type-A blood";
	private static final List<Word> EXPECTED_HYPHENATED_TRAILING_SINGLE_CHAR = List.of(Word.of("Type", 0), Word.of("blood", 7));

	@Test
	public void constructorThrowsOnNull() {
		assertThrows(NullPointerException.class, () -> new DefaultTokenizer(Locale.ENGLISH, null));
		return;
	}

	@Test
	public void DefaultTokenizerRemovesPunct() {
		testTokenizerAndStringList(new DefaultTokenizer(Locale.ENGLISH, TEST_1), EXPECTED_1);
		return;
	}

	@Test
	public void DefaultTokenizerHandlesSomeSpecialCases() {
		testTokenizerAndStringList(new DefaultTokenizer(Locale.ENGLISH, TEST_2), EXPECTED_2);
		return;
	}

	@Test
	public void tokenizerRecordsExpectedIndexes() {
		testTokenizerAndWordList(new DefaultTokenizer(Locale.ENGLISH, TEST_3), EXPECTED_3);
	}

	@Test
	public void tokenizerRecordsExpectedIndexesForSpecialCases() {
		testTokenizerAndWordList(new DefaultTokenizer(Locale.ENGLISH, TEST_4), EXPECTED_4);
		testTokenizerAndWordList(new DefaultTokenizer(Locale.ENGLISH, TEST_5), EXPECTED_5);
		return;
	}

	private void testTokenizerAndStringList(Tokenizer tokenizer, List<String> words) {
        for (String word : words) {
            assertEquals(word, tokenizer.next().text());
        }
		return;
	}

	private void testTokenizerAndWordList(Tokenizer tokenizer, List<Word> words) {
        for (Word word : words) {
            assertEquals(word, tokenizer.next());
        }
		return;
	}

	/**
	 * Simply runs {@code tokenizer} until there are no more tokens.
	 *
	 * @param tokenizer a {@link Tokenizer}
	 */
	private void testTokenizer(Tokenizer tokenizer) {
		while (tokenizer.hasNext()) {
			tokenizer.next();
		}
		return;
	}

	// This method just has to run without causing StringIndexOutOfBoundsException
	@Test
	public void whileLoopWithHasNextWillReadAllTokens() {
		Tokenizer tokenizer = new DefaultTokenizer(Locale.ENGLISH, TEST_1);
		while (tokenizer.hasNext()) {
			tokenizer.next();
		}
		return;
	}

	@Test
	public void hasNextReturnsFalseForBlankString() {
		Tokenizer tokenizer = new DefaultTokenizer(Locale.ENGLISH, BLANK_1);
		assertFalse(tokenizer.hasNext());
		tokenizer = new DefaultTokenizer(Locale.ENGLISH, BLANK_2);
		assertFalse(tokenizer.hasNext());
		tokenizer = new DefaultTokenizer(Locale.ENGLISH, BLANK_3);
		assertFalse(tokenizer.hasNext());
		tokenizer = new DefaultTokenizer(Locale.ENGLISH, BLANK_4);
		assertFalse(tokenizer.hasNext());
		return;
	}

	@Test
	public void tokenizerHandlesMixedDashesAndSpecialCases() {
		testTokenizerAndWordList(new DefaultTokenizer(Locale.ENGLISH, TEST_6), EXPECTED_6);
		return;
	}

	@Test
	public void tokenizerHandlesApostrophes() {
		Tokenizer tokenizer = new DefaultTokenizer(Locale.ENGLISH, TEST_7);
		testTokenizerAndWordList(tokenizer, EXPECTED_7);
		assertEquals(COOKED_7, tokenizer.text());
		assertEquals(TEST_7, tokenizer.rawText());
		testTokenizerAndWordList(new DefaultTokenizer(Locale.ENGLISH, TEST_8), EXPECTED_8);
		return;
	}

	// Another test for dash removal after breaking the parser.
	@Test
	public void tokenizerHandlesDashes() {
		testTokenizerAndWordList(new DefaultTokenizer(Locale.ENGLISH, TEST_9), EXPECTED_9);
		return;
	}

	// Tokenizer seems to be choking on timestamps
	@Test
	public void tokenizerShouldCopeWithTimestamp() {
		testTokenizerAndWordList(new DefaultTokenizer(Locale.ENGLISH, TEST_10), EXPECTED_10);
		return;
	}

	// "E.g." and "I.e."
	@Test
	public void tokenizerHandlesSpecialCasesWithInitialCaps() {
		testTokenizerAndWordList(new DefaultTokenizer(Locale.ENGLISH, TEST_11), EXPECTED_11);
		return;
	}

	// https://github.com/logicsquad/ibis/issues/5
	@Test
	public void tokenizerHandlesOddCasesWithDashes() {
		testTokenizer(new DefaultTokenizer(Locale.ENGLISH, EM_DASH_1));
		testTokenizer(new DefaultTokenizer(Locale.ENGLISH, EM_DASH_2));
		testTokenizer(new DefaultTokenizer(Locale.ENGLISH, EM_DASH_3));
		testTokenizer(new DefaultTokenizer(Locale.ENGLISH, EM_DASH_4));
		return;
	}

	/**
	 * Returns all remaining {@link Word}s from {@code tokenizer}.
	 *
	 * @param tokenizer a {@link Tokenizer}
	 * @return list of remaining {@link Word}s
	 */
	private List<Word> allWords(Tokenizer tokenizer) {
		List<Word> result = new ArrayList<>();
		while (tokenizer.hasNext()) {
			result.add(tokenizer.next());
		}
		return result;
	}

	// https://github.com/logicsquad/ibis/issues/7
	@Test
	public void tokenizerContinuesAfterDroppingHandledWord() {
		assertEquals(EXPECTED_POSSESSIVE_SINGLE_CHAR, allWords(new DefaultTokenizer(Locale.ENGLISH, POSSESSIVE_SINGLE_CHAR)));
		assertEquals(EXPECTED_HYPHENATED_SINGLE_CHARS, allWords(new DefaultTokenizer(Locale.ENGLISH, HYPHENATED_SINGLE_CHARS)));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/7
	@Test
	public void tokenizerDropsSingleCharPartOfHyphenatedWord() {
		assertEquals(EXPECTED_HYPHENATED_TRAILING_SINGLE_CHAR, allWords(new DefaultTokenizer(Locale.ENGLISH, HYPHENATED_TRAILING_SINGLE_CHAR)));
		return;
	}
}
