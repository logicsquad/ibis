package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TokenizerTest {
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
	private static final Word WORD_6E = Word.of("a", 17);
	private static final Word WORD_6F = Word.of("tit", 19);
	private static final Word WORD_6G = Word.of("for", 23);
	private static final Word WORD_6H = Word.of("tat", 27);
	private static final Word WORD_6I = Word.of("crazy", 31);
	private static final Word WORD_6J = Word.of("one", 37);
	private static final Word WORD_6K = Word.of("two", 41);
	private static final Word WORD_6L = Word.of("example", 45);
	private static final List<Word> EXPECTED_6 = List.of(WORD_6A, WORD_6B, WORD_6C, WORD_6D, WORD_6E, WORD_6F, WORD_6G, WORD_6H, WORD_6I, WORD_6J, WORD_6K, WORD_6L);


	@Test
	public void constructorThrowsOnNull() {
		assertThrows(NullPointerException.class, () -> new Tokenizer(null));
		return;
	}

	@Test
	public void tokenizerRemovesPunct() {
		testTokenizerAndStringList(new Tokenizer(TEST_1), EXPECTED_1);
		return;
	}

	@Test
	public void tokenizerHandlesSomeSpecialCases() {
		testTokenizerAndStringList(new Tokenizer(TEST_2), EXPECTED_2);
		return;
	}

	@Test
	public void tokenizerRecordsExpectedIndexes() {
		testTokenizerAndWordList(new Tokenizer(TEST_3), EXPECTED_3);
	}

	@Test
	public void tokenizerRecordsExpectedIndexesForSpecialCases() {
		testTokenizerAndWordList(new Tokenizer(TEST_4), EXPECTED_4);
		testTokenizerAndWordList(new Tokenizer(TEST_5), EXPECTED_5);
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

	// This method just has to run without causing StringIndexOutOfBoundsException
	@Test
	public void whileLoopWithHasNextWillReadAllTokens() {
		Tokenizer tokenizer = new Tokenizer(TEST_1);
		while (tokenizer.hasNext()) {
			tokenizer.next();
		}
		return;
	}

	@Test
	public void hasNextReturnsFalseForBlankString() {
		Tokenizer tokenizer = new Tokenizer(BLANK_1);
		assertFalse(tokenizer.hasNext());
		tokenizer = new Tokenizer(BLANK_2);
		assertFalse(tokenizer.hasNext());
		tokenizer = new Tokenizer(BLANK_3);
		assertFalse(tokenizer.hasNext());
		tokenizer = new Tokenizer(BLANK_4);
		assertFalse(tokenizer.hasNext());
		return;
	}

	@Test
	public void tokenizerHandlesMixedDashesAndSpecialCases() {
		testTokenizerAndWordList(new Tokenizer(TEST_6), EXPECTED_6);
		return;
	}
}
