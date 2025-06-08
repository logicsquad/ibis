package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		for (int i = 0; i < words.size(); i++) {
			assertEquals(words.get(i), tokenizer.next().text());
		}
		return;
	}

	private void testTokenizerAndWordList(Tokenizer tokenizer, List<Word> words) {
		for (int i = 0; i < words.size(); i++) {
			assertEquals(words.get(i), tokenizer.next());
		}
		return;
	}
}
