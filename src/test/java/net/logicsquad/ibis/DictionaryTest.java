package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests on {@link Dictionary}.
 * 
 * @author paulh
 */
public class DictionaryTest {
	// A plain text word list with a name that says it's compressed
	private static final String NOT_GZIPPED_RESOURCE = "/not-gzipped.txt.gz";

	// A word list encoded in ISO-8859-1, which is not valid UTF-8
	private static final String LATIN_1_RESOURCE = "/latin-1.txt";

	private static Dictionary dictionary;

	@BeforeAll
	public static void setup() {
		dictionary = Dictionary.builder().addWord("alpha").addWord("beta").build();
		return;
	}

	@Test
	public void isCorrectReturnsTrueIfWordPresent() {
		assertTrue(dictionary.isCorrect("alpha"));
		assertTrue(dictionary.isCorrect("beta"));
		return;
	}

	@Test
	public void isCorrectReturnsFalseIfWordNotPresent() {
		assertFalse(dictionary.isCorrect("gamma"));
		assertFalse(dictionary.isCorrect("delta"));
		return;
	}

	// The problem here is that "AUSTRALIAN" is being flagged incorrect, even though "Australian" is in the word list.
	@Test
	public void isCorrectShouldTryInitialCaps() {
		Dictionary d = Dictionary.builder().addWord("Australian").build();
		assertTrue(d.isCorrect("Australian"));
		assertTrue(d.isCorrect("AUSTRALIAN"));
		return;
	}

	// "why" has an empty Metaphone code, so an empty string is looked up among its neighbours
	// https://github.com/logicsquad/ibis/issues/8
	@Test
	public void isCorrectReturnsFalseForEmptyWord() {
		Dictionary d = Dictionary.builder().addWord("why").build();
		assertFalse(d.isCorrect(""));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/9
	@Test
	public void addBuiltInWordsAddsWordsForAnyEnglishLocale() {
		for (Locale locale : List.of(Locale.ENGLISH, Locale.US, Locale.UK, Locale.forLanguageTag("en-AU"))) {
			assertTrue(Dictionary.builder(locale).addBuiltInWords().build().isCorrect("the"), locale.toString());
		}
		return;
	}

	// https://github.com/logicsquad/ibis/issues/9
	@Test
	public void builderWithoutLocaleAddsEnglishBuiltInWords() {
		assertTrue(Dictionary.builder().addBuiltInWords().build().isCorrect("the"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/10
	@Test
	public void isCorrectIgnoresDefaultLocale() {
		Dictionary d = Dictionary.builder().addWord("it").addWord("inside").build();
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr"));
		try {
			assertTrue(d.isCorrect("It"));
			assertTrue(d.isCorrect("INSIDE"));
		} finally {
			Locale.setDefault(defaultLocale);
		}
		return;
	}

	// https://github.com/logicsquad/ibis/issues/11
	@Test
	public void addingWordsAfterBuildDoesNotChangeDictionary() {
		Dictionary.Builder builder = Dictionary.builder().addWord("alpha");
		Dictionary d = builder.build();
		// "alfa" has the same Metaphone code as "alpha", and "omega" has a new one
		builder.addWord("alfa").addWord("omega");
		assertFalse(d.isCorrect("alfa"));
		assertFalse(d.isCorrect("omega"));
		assertTrue(builder.build().isCorrect("omega"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsThrowsForMissingResource() {
		assertThrows(UncheckedIOException.class, () -> Dictionary.builder().addWords(DictionaryTest.class, "/no/such/resource.txt"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsThrowsForMissingPath(@TempDir Path dir) {
		assertThrows(UncheckedIOException.class, () -> Dictionary.builder().addWords(dir.resolve("missing.txt")));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/20
	@Test
	public void addWordsReadsPlainTextResourceWithGzipName() {
		assertTrue(Dictionary.builder().addWords(DictionaryTest.class, NOT_GZIPPED_RESOURCE).build().isCorrect("zyzzyva"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsThrowsForMalformedUtf8() {
		assertThrows(UncheckedIOException.class, () -> Dictionary.builder().addWords(DictionaryTest.class, LATIN_1_RESOURCE));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsThrowsForMalformedUtf8InPath(@TempDir Path dir) throws IOException {
		Path path = dir.resolve("latin-1.txt");
		Files.write(path, "café\n".getBytes(StandardCharsets.ISO_8859_1));
		assertThrows(UncheckedIOException.class, () -> Dictionary.builder().addWords(path));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsDecompressesGzippedPath(@TempDir Path dir) throws IOException {
		Path path = dir.resolve("words.txt.gz");
		try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(path))) {
			out.write("zyzzyva\n".getBytes(StandardCharsets.UTF_8));
		}
		assertTrue(Dictionary.builder().addWords(path).build().isCorrect("zyzzyva"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsThrowsForUnreadableReader() {
		Reader reader = new Reader() {
			@Override
			public int read(char[] buffer, int offset, int length) throws IOException {
				throw new IOException("Simulated failure");
			}

			@Override
			public void close() {
				return;
			}
		};
		assertThrows(UncheckedIOException.class, () -> Dictionary.builder().addWords(reader));
		return;
	}

	@Test
	public void toLowerCaseReturnsExpectedResult() {
		assertEquals("expected", Dictionary.toLowerCase("expected", Locale.ENGLISH));
		assertEquals("expected", Dictionary.toLowerCase("Expected", Locale.ENGLISH));
		assertEquals("expected", Dictionary.toLowerCase("EXPECTED", Locale.ENGLISH));
		assertEquals("expected", Dictionary.toLowerCase("eXPECted", Locale.ENGLISH));
		assertEquals("expected", Dictionary.toLowerCase("expecteD", Locale.ENGLISH));
		return;
	}

	@Test
	public void toInitialCapReturnsExpectedResult() {
		assertEquals("Expected", Dictionary.toInitialCap("expected", Locale.ENGLISH));
		assertEquals("Expected", Dictionary.toInitialCap("Expected", Locale.ENGLISH));
		assertEquals("Expected", Dictionary.toInitialCap("EXPECTED", Locale.ENGLISH));
		assertEquals("Expected", Dictionary.toInitialCap("eXPECted", Locale.ENGLISH));
		assertEquals("Expected", Dictionary.toInitialCap("expecteD", Locale.ENGLISH));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/8
	@Test
	public void toInitialCapReturnsEmptyStringForEmptyText() {
		assertEquals("", Dictionary.toInitialCap("", Locale.ENGLISH));
		return;
	}

	// In Turkish, "I" lowercases to dotless "ı", and "i" uppercases to dotted "İ"
	// https://github.com/logicsquad/ibis/issues/10
	@Test
	public void caseConversionIgnoresDefaultLocale() {
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr"));
		try {
			assertEquals("inside", Dictionary.toLowerCase("INSIDE", Locale.ENGLISH));
			assertEquals("Inside", Dictionary.toInitialCap("INSIDE", Locale.ENGLISH));
			assertEquals("Inside", Dictionary.toInitialCap("inside", Locale.ENGLISH));
		} finally {
			Locale.setDefault(defaultLocale);
		}
		return;
	}

	// https://github.com/logicsquad/ibis/issues/19
	@Test
	public void suggestionsForReturnsEmptyListForCorrectWord() {
		assertEquals(List.of(), dictionary.suggestionsFor("alpha"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/15
	@Test
	public void localeReturnsBuilderLocale() {
		assertEquals(Locale.UK, Dictionary.builder(Locale.UK).build().locale());
		assertEquals(Locale.ENGLISH, Dictionary.builder().build().locale());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/15
	@Test
	public void builderThrowsForUnsupportedLanguage() {
		assertThrows(IllegalArgumentException.class, () -> Dictionary.builder(Locale.GERMAN));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/20
	@Test
	public void addWordsDecompressesGzippedStreamWhateverItsName(@TempDir Path dir) throws IOException {
		Path path = dir.resolve("words.txt");
		try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(path))) {
			out.write("zyzzyva\n".getBytes(StandardCharsets.UTF_8));
		}
		assertTrue(Dictionary.builder().addWords(path).build().isCorrect("zyzzyva"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/20
	@Test
	public void addWordsLeavesInputStreamOpen() {
		AtomicBoolean closed = new AtomicBoolean();
		InputStream is = new ByteArrayInputStream("zyzzyva\n".getBytes(StandardCharsets.UTF_8)) {
			@Override
			public void close() {
				closed.set(true);
				return;
			}
		};
		assertTrue(Dictionary.builder().addWords(is).build().isCorrect("zyzzyva"));
		assertFalse(closed.get());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/20
	@Test
	public void addWordsLeavesReaderOpen() {
		AtomicBoolean closed = new AtomicBoolean();
		Reader reader = new StringReader("zyzzyva\n") {
			@Override
			public void close() {
				closed.set(true);
				return;
			}
		};
		assertTrue(Dictionary.builder().addWords(reader).build().isCorrect("zyzzyva"));
		assertFalse(closed.get());
		return;
	}

	// https://github.com/logicsquad/ibis/issues/20
	@Test
	public void addWordsAddsCollection() {
		Dictionary d = Dictionary.builder().addWords(List.of("zyzzyva", " aardvark ")).build();
		assertTrue(d.isCorrect("zyzzyva"));
		assertTrue(d.isCorrect("aardvark"));
		return;
	}

	// A relative resource name resolves in the package of the context class
	// https://github.com/logicsquad/ibis/issues/20
	@Test
	public void addWordsResolvesRelativeResourceNameThroughContext() {
		assertThrows(UncheckedIOException.class, () -> Dictionary.builder().addWords(DictionaryTest.class, NOT_GZIPPED_RESOURCE.substring(1)));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/21
	@Test
	public void builtInIsSharedByLanguage() {
		Dictionary builtIn = Dictionary.builtIn(Locale.ENGLISH);
		assertSame(builtIn, Dictionary.builtIn(Locale.UK));
		assertEquals(Locale.ENGLISH, Dictionary.builtIn(Locale.US).locale());
		assertTrue(builtIn.isCorrect("the"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/21
	@Test
	public void builtInThrowsForUnsupportedLanguage() {
		assertThrows(IllegalArgumentException.class, () -> Dictionary.builtIn(Locale.GERMAN));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/21
	@Test
	public void withAddsWordsWithoutChangingOriginal() {
		Dictionary d = dictionary.with(List.of("gamma"));
		assertTrue(d.isCorrect("alpha"));
		assertTrue(d.isCorrect("gamma"));
		assertFalse(dictionary.isCorrect("gamma"));
		return;
	}

	// "alfa" has the same Metaphone code as "alpha", so both are suggestions for "allfa", and "alfa" is closer
	// https://github.com/logicsquad/ibis/issues/21
	@Test
	public void withMergesSuggestions() {
		Dictionary d = dictionary.with(List.of("alfa", "alpha"));
		assertEquals(List.of("alfa", "alpha"), d.suggestionsFor("allfa"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/21
	@Test
	public void addBuiltInWordsKeepsOtherWords() {
		Dictionary d = Dictionary.builder(Locale.UK).addWord("zyzzyva").addBuiltInWords().build();
		assertTrue(d.isCorrect("the"));
		assertTrue(d.isCorrect("zyzzyva"));
		assertEquals(Locale.UK, d.locale());
		assertFalse(Dictionary.builtIn(Locale.UK).isCorrect("zyzzyva"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/22
	@Test
	public void suggestionsForPutsIntendedWordFirst() {
		Dictionary d = Dictionary.builtIn(Locale.ENGLISH);
		assertEquals("for", d.suggestionsFor("forr").get(0));
		assertEquals("definitely", d.suggestionsFor("definately").get(0));
		assertEquals("weird", d.suggestionsFor("wierd").get(0));
		assertEquals("the", d.suggestionsFor("teh").get(0));
		assertEquals("and", d.suggestionsFor("adn").get(0));
		return;
	}

	// None of these is in the frequency list, so they are equally common
	// https://github.com/logicsquad/ibis/issues/22
	@Test
	public void suggestionsForOrdersByDistanceThenAlphabetically() {
		Dictionary d = Dictionary.builder().addWords(List.of("Zora", "zore", "zair", "zor", "zar")).build();
		assertEquals(List.of("zor", "Zora", "zore", "zair", "zar"), d.suggestionsFor("zorr"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/27
	@Test
	public void suggestionsForPrefersCommonWordsAtSameDistance() {
		Dictionary d = Dictionary.builder().addWords(List.of("fora", "fore")).build();
		assertEquals(List.of("fore", "fora"), d.suggestionsFor("forr"));
		return;
	}

	// "tha" is one edit from "thaa", and "the" two, but "the" is far more common
	// https://github.com/logicsquad/ibis/issues/27
	@Test
	public void suggestionsForCanPreferMuchMoreCommonWordsFurtherAway() {
		Dictionary d = Dictionary.builder().addWords(List.of("the", "tha")).build();
		assertEquals(List.of("the", "tha"), d.suggestionsFor("thaa"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/22
	@Test
	public void suggestionsForIgnoresCase() {
		Dictionary d = Dictionary.builder().addWord("NATO").build();
		assertEquals(List.of("NATO"), d.suggestionsFor("nato"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/22
	@Test
	public void suggestionsForReturnsAtMostTen() {
		Dictionary d = Dictionary.builtIn(Locale.ENGLISH);
		assertTrue(d.suggestionsFor("forr", 100).size() > 10);
		assertEquals(d.suggestionsFor("forr", 100).subList(0, 10), d.suggestionsFor("forr"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/22
	@Test
	public void suggestionsForRespectsLimit() {
		Dictionary d = Dictionary.builtIn(Locale.ENGLISH);
		assertEquals(List.of("for", "four"), d.suggestionsFor("forr", 2));
		assertEquals(List.of(), d.suggestionsFor("forr", 0));
		assertEquals(List.of(), d.suggestionsFor("for", 2));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/22
	@Test
	public void suggestionsForThrowsForNegativeLimit() {
		assertThrows(IllegalArgumentException.class, () -> dictionary.suggestionsFor("allfa", -1));
		return;
	}

	// "the" doesn't sound like "teh", and "government" doesn't sound like "goverment", but each is one edit away
	// https://github.com/logicsquad/ibis/issues/23
	@Test
	public void suggestionsForIncludesWordsOneEditAway() {
		Dictionary d = Dictionary.builtIn(Locale.ENGLISH);
		assertTrue(d.suggestionsFor("teh").contains("the"));
		assertEquals("government", d.suggestionsFor("goverment").get(0));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/23
	@Test
	public void suggestionsForFindsEachKindOfEdit() {
		Dictionary d = Dictionary.builder().addWords(List.of("the", "government", "pasta", "attic")).build();
		assertEquals(List.of("the"), d.suggestionsFor("teh"));
		assertEquals(List.of("government"), d.suggestionsFor("goverment"));
		assertEquals(List.of("pasta"), d.suggestionsFor("pastxa"));
		assertEquals(List.of("attic"), d.suggestionsFor("attiq"));
		return;
	}

	// "ten" is one edit from "teh", and more common, but "tea" also sounds like it
	// https://github.com/logicsquad/ibis/issues/23
	@Test
	public void suggestionsForPrefersWordsThatSoundAlikeAtSameDistance() {
		Dictionary d = Dictionary.builder().addWords(List.of("ten", "tea")).build();
		assertEquals(List.of("tea", "ten"), d.suggestionsFor("teh"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/23
	@Test
	public void suggestionsForGivesWordsDifferingInCaseOnce() {
		assertEquals(List.of("ward"), Dictionary.builder().addWords(List.of("Ward", "ward")).build().suggestionsFor("wardd"));
		assertEquals(List.of("ward"), Dictionary.builder().addWords(List.of("ward", "Ward")).build().suggestionsFor("wardd"));
		assertEquals(List.of("Ward"), Dictionary.builder().addWord("Ward").build().suggestionsFor("wardd"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/23
	@Test
	public void suggestionsForUsesWordsInBase() {
		Dictionary d = Dictionary.builder().addWord("the").build().with(List.of("tea"));
		assertEquals(List.of("the", "tea"), d.suggestionsFor("teh"));
		return;
	}
}
