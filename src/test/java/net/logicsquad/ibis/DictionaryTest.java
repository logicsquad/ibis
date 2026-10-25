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
}
