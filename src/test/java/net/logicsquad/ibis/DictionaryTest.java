package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
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
		assertTrue(dictionary.isCorrect(new Word("alpha", 0)));
		assertTrue(dictionary.isCorrect(new Word("beta", 0)));
		return;
	}

	@Test
	public void isCorrectReturnsFalseIfWordNotPresent() {
		assertFalse(dictionary.isCorrect(new Word("gamma", 0)));
		assertFalse(dictionary.isCorrect(new Word("delta", 0)));
		return;
	}

	// The problem here is that "AUSTRALIAN" is being flagged incorrect, even though "Australian" is in the word list.
	@Test
	public void isCorrectShouldTryInitialCaps() {
		Dictionary d = Dictionary.builder().addWord("Australian").build();
		assertTrue(d.isCorrect(new Word("Australian", 0)));
		assertTrue(d.isCorrect(new Word("AUSTRALIAN", 0)));
		return;
	}

	// "why" has an empty Metaphone code, so an empty Word is looked up among its neighbours
	// https://github.com/logicsquad/ibis/issues/8
	@Test
	public void isCorrectReturnsFalseForEmptyWord() {
		Dictionary d = Dictionary.builder().addWord("why").build();
		assertFalse(d.isCorrect(new Word("", 0)));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/9
	@Test
	public void addWordsAddsBuiltInWordsForAnyEnglishLocale() {
		for (Locale locale : List.of(Locale.ENGLISH, Locale.US, Locale.UK, Locale.forLanguageTag("en-AU"))) {
			assertTrue(Dictionary.builder(locale).addWords().build().isCorrect(new Word("the", 0)), locale.toString());
		}
		return;
	}

	// https://github.com/logicsquad/ibis/issues/9
	@Test
	public void builderWithoutLocaleAddsEnglishBuiltInWords() {
		assertTrue(Dictionary.builder().addWords().build().isCorrect(new Word("the", 0)));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/10
	@Test
	public void isCorrectIgnoresDefaultLocale() {
		Dictionary d = Dictionary.builder().addWord("it").addWord("inside").build();
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr"));
		try {
			assertTrue(d.isCorrect(new Word("It", 0)));
			assertTrue(d.isCorrect(new Word("INSIDE", 0)));
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
		assertFalse(d.isCorrect(new Word("alfa", 0)));
		assertFalse(d.isCorrect(new Word("omega", 0)));
		assertTrue(builder.build().isCorrect(new Word("omega", 0)));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsThrowsForMissingResource() {
		assertThrows(IllegalArgumentException.class, () -> Dictionary.builder().addWords("/no/such/resource.txt"));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsThrowsForMissingPath(@TempDir Path dir) {
		assertThrows(IllegalArgumentException.class, () -> Dictionary.builder().addWords(dir.resolve("missing.txt")));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsThrowsForResourceThatIsNotGzipped() {
		assertThrows(IllegalArgumentException.class, () -> Dictionary.builder().addWords(NOT_GZIPPED_RESOURCE));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsThrowsForMalformedUtf8() {
		assertThrows(IllegalArgumentException.class, () -> Dictionary.builder().addWords(LATIN_1_RESOURCE));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsThrowsForMalformedUtf8InPath(@TempDir Path dir) throws IOException {
		Path path = dir.resolve("latin-1.txt");
		Files.write(path, "café\n".getBytes(StandardCharsets.ISO_8859_1));
		assertThrows(IllegalArgumentException.class, () -> Dictionary.builder().addWords(path));
		return;
	}

	// https://github.com/logicsquad/ibis/issues/13
	@Test
	public void addWordsDecompressesGzippedPath(@TempDir Path dir) throws IOException {
		Path path = dir.resolve("words.txt.gz");
		try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(path))) {
			out.write("zyzzyva\n".getBytes(StandardCharsets.UTF_8));
		}
		assertTrue(Dictionary.builder().addWords(path).build().isCorrect(new Word("zyzzyva", 0)));
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
		assertThrows(IllegalArgumentException.class, () -> Dictionary.builder().addWords(reader));
		return;
	}
}
