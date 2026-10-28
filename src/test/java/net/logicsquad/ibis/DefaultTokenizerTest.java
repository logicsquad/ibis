package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link DefaultTokenizer}.
 * 
 * @author paulh
 */
public class DefaultTokenizerTest {
	private static final Tokenizer TOKENIZER = new DefaultTokenizer(Locale.ENGLISH);

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
	private static final List<Word> EXPECTED_3 = List.of(new Word("Now", 0), new Word("is", 4), new Word("the", 7), new Word("time", 11));

	private static final String TEST_4 = "Here is my—em dash";
	private static final List<Word> EXPECTED_4 = List.of(new Word("Here", 0), new Word("is", 5), new Word("my", 8), new Word("em", 11), new Word("dash", 14));

	private static final String TEST_5 = "Now e.g. red one.";
	private static final List<Word> EXPECTED_5 = List.of(new Word("Now", 0), new Word("e.g.", 4), new Word("red", 9), new Word("one", 13));

	private static final String BLANK_1 = "";
	private static final String BLANK_2 = "   ";
	private static final String BLANK_3 = "\n";
	private static final String BLANK_4 = "\t\n ";

	private static final String TEST_6 = "Here—e.g. one—is a tit-for-tat—crazy one–two—example!";
	private static final Word WORD_6A = new Word("Here", 0);
	private static final Word WORD_6B = new Word("e.g.", 5);
	private static final Word WORD_6C = new Word("one", 10);
	private static final Word WORD_6D = new Word("is", 14);
	private static final Word WORD_6F = new Word("tit", 19);
	private static final Word WORD_6G = new Word("for", 23);
	private static final Word WORD_6H = new Word("tat", 27);
	private static final Word WORD_6I = new Word("crazy", 31);
	private static final Word WORD_6J = new Word("one", 37);
	private static final Word WORD_6K = new Word("two", 41);
	private static final Word WORD_6L = new Word("example", 45);
	private static final List<Word> EXPECTED_6 = List.of(WORD_6A, WORD_6B, WORD_6C, WORD_6D, WORD_6F, WORD_6G, WORD_6H, WORD_6I, WORD_6J, WORD_6K,
			WORD_6L);

	private static final String TEST_7 = "It looks like the client’s, doesn't it?";
	private static final List<Word> EXPECTED_7 = List.of(new Word("It", 0), new Word("looks", 3), new Word("like", 9), new Word("the", 14), new Word("client", 18),
			new Word("doesn't", 28), new Word("it", 36));

	private static final String TEST_8 = "“‘we'll’, ‘they‘ll’, ‘don՚t’, ‘can＇t’, ‘weߴre’, ‘wonʼt’";
	private static final List<Word> EXPECTED_8 = List.of(new Word("we'll", 2), new Word("they'll", 11), new Word("don't", 22), new Word("can't", 31),
			new Word("we're", 40), new Word("won't", 49));

	private static final String TEST_9 = "alpha—beta gamma";
	private static final List<Word> EXPECTED_9 = List.of(new Word("alpha", 0), new Word("beta", 6), new Word("gamma", 11));

	private static final String TEST_10 = "one 2022-11-03T02:09:00Z two";
	private static final List<Word> EXPECTED_10 = List.of(new Word("one", 0), new Word("two", 25));

	// "E.g." and "I.e." should be handled as special cases even with initial caps
	private static final String TEST_11 = "(E.g. one.) (I.e. another.)";
	private static final List<Word> EXPECTED_11 = List.of(new Word("E.g.", 1), new Word("one", 6), new Word("I.e.", 13), new Word("another", 18));

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
	private static final List<Word> EXPECTED_POSSESSIVE_SINGLE_CHAR = List.of(new Word("Grade", 0), new Word("are", 10), new Word("fine", 14));
	private static final String HYPHENATED_SINGLE_CHARS = "We did A-B testing.";
	private static final List<Word> EXPECTED_HYPHENATED_SINGLE_CHARS = List.of(new Word("We", 0), new Word("did", 3), new Word("testing", 11));
	private static final String HYPHENATED_TRAILING_SINGLE_CHAR = "Type-A blood";
	private static final List<Word> EXPECTED_HYPHENATED_TRAILING_SINGLE_CHAR = List.of(new Word("Type", 0), new Word("blood", 7));

	// Special cases at the very end of the text, with no character after them
	private static final String ABBREVIATION_AT_END_1 = "for example, e.g";
	private static final List<Word> EXPECTED_ABBREVIATION_AT_END_1 = List.of(new Word("for", 0), new Word("example", 4), new Word("e.g", 13));
	private static final String ABBREVIATION_AT_END_2 = "that is, i.e";
	private static final List<Word> EXPECTED_ABBREVIATION_AT_END_2 = List.of(new Word("that", 0), new Word("is", 5), new Word("i.e", 9));

	// URLs and email addresses, whose parts aren't words to check
	private static final String URL_1 = "See https://www.legislation.gov.au/F2024L00996/asmade/text for details.";
	private static final List<Word> EXPECTED_URL_1 = List.of(new Word("See", 0), new Word("for", 59), new Word("details", 63));
	private static final String URL_2 = "Try http://example.com/path?query=abc#frag now.";
	private static final List<String> EXPECTED_URL_2 = List.of("Try", "now");
	private static final String URL_3 = "Visit www.oaic.gov.au/privacy/your-privacy-rights today.";
	private static final List<String> EXPECTED_URL_3 = List.of("Visit", "today");
	private static final String URL_4 = "(See HTTPS://EXAMPLE.COM/a), then stop.";
	private static final List<String> EXPECTED_URL_4 = List.of("See", "then", "stop");
	private static final String URL_5 = "Fetch ftp://files.example.com/pub/notes.txt first.";
	private static final List<String> EXPECTED_URL_5 = List.of("Fetch", "first");
	private static final String EMAIL = "Email privacy@logicsquad.net, or write to mailto:help@example.com today.";
	private static final List<Word> EXPECTED_EMAIL = List.of(new Word("Email", 0), new Word("or", 30), new Word("write", 33), new Word("to", 39),
			new Word("today", 66));

	// A colon alone doesn't make a URL
	private static final String COLONS = "Note:this and Note: that";
	private static final List<String> EXPECTED_COLONS = List.of("Note", "this", "and", "Note", "that");

	// Outside a URL, a protocol name is a word like any other
	private static final String PROTOCOL_NAMES = "Served over https, not http or ftp.";
	private static final List<String> EXPECTED_PROTOCOL_NAMES = List.of("Served", "over", "https", "not", "http", "or", "ftp");

	// A long run of the characters that URLs and email addresses are made of, which a careless pattern scans in quadratic time
	private static final String LONG_RUN = "a.".repeat(100_000);

	@Test
	public void tokenizeThrowsOnNull() {
		assertThrows(NullPointerException.class, () -> TOKENIZER.tokenize(null));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/17
	@Test
	public void tokenizerCanBeReused() {
		testTokenizerAndWordList(TEST_3, EXPECTED_3);
		testTokenizerAndWordList(TEST_3, EXPECTED_3);
		return;
	}

	@Test
	public void DefaultTokenizerRemovesPunct() {
		testTokenizerAndStringList(TEST_1, EXPECTED_1);
		return;
	}

	@Test
	public void DefaultTokenizerHandlesSomeSpecialCases() {
		testTokenizerAndStringList(TEST_2, EXPECTED_2);
		return;
	}

	@Test
	public void tokenizerRecordsExpectedIndexes() {
		testTokenizerAndWordList(TEST_3, EXPECTED_3);
	}

	@Test
	public void tokenizerRecordsExpectedIndexesForSpecialCases() {
		testTokenizerAndWordList(TEST_4, EXPECTED_4);
		testTokenizerAndWordList(TEST_5, EXPECTED_5);
		return;
	}

	private void testTokenizerAndStringList(String text, List<String> words) {
		assertEquals(words, TOKENIZER.tokenize(text).map(Word::text).toList());
		return;
	}

	private void testTokenizerAndWordList(String text, List<Word> words) {
		assertEquals(words, TOKENIZER.tokenize(text).toList());
		return;
	}

	/**
	 * Simply runs {@link #TOKENIZER} over {@code text}.
	 * @param text a string
	 */
	private void testTokenizer(String text) {
		TOKENIZER.tokenize(text).forEach(w -> {});
		return;
	}

	// This method just has to run without causing StringIndexOutOfBoundsException
	@Test
	public void tokenizeWillReadAllTokens() {
		testTokenizer(TEST_1);
		return;
	}

	@Test
	public void tokenizeReturnsNoWordsForBlankString() {
		for (String blank : List.of(BLANK_1, BLANK_2, BLANK_3, BLANK_4)) {
			assertEquals(0, TOKENIZER.tokenize(blank).count());
		}
		return;
	}

	@Test
	public void tokenizerHandlesMixedDashesAndSpecialCases() {
		testTokenizerAndWordList(TEST_6, EXPECTED_6);
		return;
	}

	@Test
	public void tokenizerHandlesApostrophes() {
		testTokenizerAndWordList(TEST_7, EXPECTED_7);
		testTokenizerAndWordList(TEST_8, EXPECTED_8);
		return;
	}

	// Another test for dash removal after breaking the parser.
	@Test
	public void tokenizerHandlesDashes() {
		testTokenizerAndWordList(TEST_9, EXPECTED_9);
		return;
	}

	// Tokenizer seems to be choking on timestamps
	@Test
	public void tokenizerShouldCopeWithTimestamp() {
		testTokenizerAndWordList(TEST_10, EXPECTED_10);
		return;
	}

	// "E.g." and "I.e."
	@Test
	public void tokenizerHandlesSpecialCasesWithInitialCaps() {
		testTokenizerAndWordList(TEST_11, EXPECTED_11);
		return;
	}

	// https://github.com/logicsquad/ibis/issues/5
	@Test
	public void tokenizerHandlesOddCasesWithDashes() {
		testTokenizer(EM_DASH_1);
		testTokenizer(EM_DASH_2);
		testTokenizer(EM_DASH_3);
		testTokenizer(EM_DASH_4);
		return;
	}

	// https://github.com/logicsquad/ibis/issues/7
	@Test
	public void tokenizerContinuesAfterDroppingHandledWord() {
		assertEquals(EXPECTED_POSSESSIVE_SINGLE_CHAR, TOKENIZER.tokenize(POSSESSIVE_SINGLE_CHAR).toList());
		assertEquals(EXPECTED_HYPHENATED_SINGLE_CHARS, TOKENIZER.tokenize(HYPHENATED_SINGLE_CHARS).toList());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/7
	@Test
	public void tokenizerDropsSingleCharPartOfHyphenatedWord() {
		assertEquals(EXPECTED_HYPHENATED_TRAILING_SINGLE_CHAR, TOKENIZER.tokenize(HYPHENATED_TRAILING_SINGLE_CHAR).toList());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/8
	@Test
	public void tokenizerHandlesAbbreviationAtEndOfText() {
		assertEquals(EXPECTED_ABBREVIATION_AT_END_1, TOKENIZER.tokenize(ABBREVIATION_AT_END_1).toList());
		assertEquals(EXPECTED_ABBREVIATION_AT_END_2, TOKENIZER.tokenize(ABBREVIATION_AT_END_2).toList());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/15
	@Test
	public void forLocaleThrowsForUnsupportedLanguage() {
		assertThrows(IllegalArgumentException.class, () -> Tokenizer.forLocale(Locale.GERMAN));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/28
	@Test
	public void tokenizerSkipsUrls() {
		testTokenizerAndWordList(URL_1, EXPECTED_URL_1);
		testTokenizerAndStringList(URL_2, EXPECTED_URL_2);
		testTokenizerAndStringList(URL_3, EXPECTED_URL_3);
		testTokenizerAndStringList(URL_4, EXPECTED_URL_4);
		testTokenizerAndStringList(URL_5, EXPECTED_URL_5);
		return;
	}

	// https://github.com/logicsquad/ibis/issues/28
	@Test
	public void tokenizerSkipsEmailAddresses() {
		testTokenizerAndWordList(EMAIL, EXPECTED_EMAIL);
		return;
	}

	// https://github.com/logicsquad/ibis/issues/28
	@Test
	public void tokenizerKeepsWordsAroundColons() {
		testTokenizerAndStringList(COLONS, EXPECTED_COLONS);
		return;
	}

	// https://github.com/logicsquad/ibis/issues/28
	@Test
	public void tokenizerKeepsProtocolNamesOutsideUrls() {
		testTokenizerAndStringList(PROTOCOL_NAMES, EXPECTED_PROTOCOL_NAMES);
		return;
	}

	// https://github.com/logicsquad/ibis/issues/28
	@Test
	public void tokenizerScansLongRunsQuickly() {
		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> testTokenizer(LONG_RUN));
		return;
	}
}
