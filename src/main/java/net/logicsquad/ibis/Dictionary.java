package net.logicsquad.ibis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.language.Metaphone;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Represents a store of known words which can:
 * </p>
 * 
 * <ul>
 * <li>indicate whether a candidate word is spelled correctly; and</li>
 * <li>provide suggestions for incorrect words.</li>
 * </ul>
 * 
 * @author paulh
 * @since 1.0
 */
public class Dictionary {
	/**
	 * Logger
	 */
	private static final Logger LOG = LoggerFactory.getLogger(Dictionary.class);

	/**
	 * Codec for providing phonetic representations
	 */
	private static final StringEncoder CODEC = new Metaphone();

	/**
	 * Maximum <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">Levenshtein distance</a> between an incorrect word and suggestions
	 * returned
	 */
	private static final int MAX_DISTANCE = 4;

	/**
	 * Map for words keyed on phonetic representation
	 */
	private final Map<String, List<String>> map;

	/**
	 * Constructor
	 * 
	 * @param map map from phonetic representations to word lists
	 */
	private Dictionary(Map<String, List<String>> map) {
		this.map = map;
		return;
	}

	/**
	 * Does {@code word} contain a word that is spelled correctly?
	 * 
	 * @param word a {@link Word}
	 * @return {@code true} if {@code word} contains a word that is spelled correctly, otherwise {@code false}
	 * @throws NullPointerException if {@code word} is {@code null}
	 */
	public boolean isCorrect(Word word) {
		Objects.requireNonNull(word);
		String code = codeForWord(word);
		if (map.containsKey(code)) {
			if (map.get(code).contains(word.text())) {
				// Verbatim
				return true;
			} else if (map.get(code).contains(word.toLowerCase())) {
				// All lower case
				return true;
			} else {
				// Initial cap
				return map.get(code).contains(word.toInitialCap());
			}
		} else {
			return false;
		}
	}

	/**
	 * Returns a list of suggestions for {@code word}.
	 * 
	 * @param word a {@link Word}
	 * @return a list of suggestions
	 * @throws IllegalArgumentException if {@code word} already contains a word spelled correctly
	 * @throws NullPointerException     if {@code word} is {@code null}
	 */
	public List<String> suggestionsFor(Word word) {
		Objects.requireNonNull(word);
		if (isCorrect(word)) {
			throw new IllegalArgumentException("word is correct.");
		}
		String code = codeForWord(word);
		if (map.containsKey(code)) {
			List<String> result = new ArrayList<>();
			for (String s : map.get(code)) {
				if (LevenshteinDistance.getDefaultInstance().apply(word.text(), s) < MAX_DISTANCE) {
					result.add(s);
				}
			}
			return result;
		} else {
			return List.of();
		}
	}

	/**
	 * Returns a code for {@code word} using {@link StringEncoder}.
	 * 
	 * @param word a word
	 * @return code for {@code word}
	 */
	private static String codeForString(String word) {
		try {
			return CODEC.encode(word);
		} catch (EncoderException e) {
			LOG.error("Unable to encode '{}'.", word, e);
			return null;
		}
	}

	/**
	 * Returns a code for {@code word} using {@link StringEncoder}.
	 * 
	 * @param word a {@link Word}
	 * @return code for {@code word}
	 */
	private static String codeForWord(Word word) {
		return codeForString(word.text());
	}

	/**
	 * Returns a new {@code Builder}.
	 * 
	 * @return a new {@code Builder}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for {@code Dictionary} objects.
	 */
	public static class Builder {
		/**
		 * World list 1
		 */
		private static final String WORDS_1 = "/words-1.txt.gz";

		/**
		 * World list 2
		 */
		private static final String WORDS_2 = "/words-2.txt";

		/**
		 * Names list 1
		 */
		private static final String NAMES_1 = "/names-1.txt.gz";

		/**
		 * Names list 2
		 */
		private static final String NAMES_2 = "/names-2.txt";

		/**
		 * Acronyms list
		 */
		private static final String ACRONYMS = "/acronyms.txt";

		/**
		 * Extension for compressed lists
		 */
		private static final String GZIP_EXTENSION = ".gz";

		/**
		 * Map from phonetic codes to lists of words
		 */
		private final Map<String, List<String>> map = new HashMap<>();

		/**
		 * Constructor
		 */
		private Builder() {
		}

		/**
		 * Adds words from all built-in word lists.
		 * 
		 * @return this object
		 */
		public Builder addWords() {
			addWords(WORDS_1);
			addWords(WORDS_2);
			addWords(NAMES_1);
			addWords(NAMES_2);
			addWords(ACRONYMS);
			return this;
		}

		/**
		 * Adds words from {@code resourceName} on classpath. The file can be compressed with GZip.
		 * 
		 * @param resourceName name of a resource on classpath
		 * @return this object
		 * @throws NullPointerException if {@code resourceName} is {@code null}
		 */
		public Builder addWords(String resourceName) {
			Objects.requireNonNull(resourceName);
			LOG.debug("Adding words from resource '{}'...", resourceName);
			try (InputStream is = Dictionary.class.getResourceAsStream(resourceName);
					Reader reader = isGzipped(resourceName) ? new InputStreamReader(new GZIPInputStream(is), StandardCharsets.UTF_8) : new InputStreamReader(is, StandardCharsets.UTF_8)) {
				addWords(reader);
			} catch (IOException e) {
				LOG.error("Unable to add words from {}.", resourceName, e);
			}
			return this;
		}

		/**
		 * Adds words from {@code reader}.
		 * 
		 * @param reader a {@link Reader}
		 * @return this object
		 * @throws NullPointerException if {@code reader} is {@code null}
		 */
		public Builder addWords(Reader reader) {
			Objects.requireNonNull(reader);
			try (BufferedReader bufferedReader = new BufferedReader(reader)) {
				bufferedReader.lines().forEach(this::addWord);
			} catch (IOException e) {
				LOG.error("Unable to add words from Reader.", e);
			}
			return this;
		}

		/**
		 * Adds words from file at {@code path}.
		 * 
		 * @param path a {@link Path}
		 * @return this object
		 * @throws NullPointerException if {@code path} is {@code null}
		 */
		public Builder addWords(Path path) {
			Objects.requireNonNull(path);
			LOG.debug("Adding words from path '{}'...", path);
			try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
				lines.forEach(this::addWord);
			} catch (IOException e) {
				LOG.error("Unable to load word list from {}.", path, e);
				throw new IllegalArgumentException("Unable to load word list from Path.", e);
			}
			return this;
		}

		/**
		 * Adds {@code word} to {@code Dictionary}. If {@code word} is an empty string after stripping whitespace, this method is a no-op.
		 * 
		 * @param word a word
		 * @return this object
		 * @throws NullPointerException if {@code word} is {@code null}
		 */
		public Builder addWord(String word) {
			Objects.requireNonNull(word);
			String cookedWord = word.strip();
			if (cookedWord.isEmpty()) {
				return this;
			}
			List<String> list = map.computeIfAbsent(codeForString(cookedWord), s -> new ArrayList<>());
			if (!list.contains(cookedWord)) {
				list.add(cookedWord);
			} else {
				LOG.debug("'{}' is already in this Dictionary.", cookedWord);
			}
			return this;
		}

		/**
		 * Does {@code resourceName} represent a file compressed with GZip?
		 * 
		 * @param resourceName resource name
		 * @return {@code true} if it looks like the file is compressed with GZip, otherwise {@code false}
		 */
		private static boolean isGzipped(String resourceName) {
			Objects.requireNonNull(resourceName);
			return resourceName.endsWith(GZIP_EXTENSION);
		}

		/**
		 * Creates and returns a new {@code Dictionary} from this {@code Builder}.
		 * 
		 * @return new {@code Dictionary}
		 */
		public Dictionary build() {
			return new Dictionary(map);
		}
	}

	/**
	 * Returns the number of <em>entries</em> (not <em>words</em>) in this {@code Dictionary}'s map.
	 * 
	 * @return count of entries in {@link #map}
	 */
	int size() {
		return map == null ? 0 : map.size();
	}
}
