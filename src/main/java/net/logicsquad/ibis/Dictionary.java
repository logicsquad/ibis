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

	private Dictionary(Map<String, List<String>> map) {
		// copy
		this.map = map;
		return;
	}

	/**
	 * Adds a word to the dictionary.
	 * 
	 * @param word a word
	 */
	public void addWord(String word) {
		List<String> list = map.computeIfAbsent(codeForString(word), s -> new ArrayList<>());
		if (!list.contains(word)) {
			list.add(word);
		}
		return;
	}

	/**
	 * Does {@code word} contain a word that is spelled correctly?
	 * 
	 * @param word a {@link Word}
	 * @return {@code true} if {@code word} contains a word that is spelled correctly, otherwise {@code false}
	 */
	public boolean isCorrect(Word word) {
		String code = codeForWord(word);
		if (map.containsKey(code)) {
			if (map.get(code).contains(word.text())) {
				return true;
			} else {
				return map.get(code).contains(word.text().toLowerCase());
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
	 */
	public List<String> suggestionsFor(Word word) {
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
	 * Returns a code for {@code word} using {@link #codec}.
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
	 * Returns a code for {@code word} using {@link #codec}.
	 * 
	 * @param word a {@link Word}
	 * @return code for {@code word}
	 */
	private static String codeForWord(Word word) {
		return codeForString(word.text());
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private static final String WORDS_1 = "/words-1.txt.gz";
		private static final String WORDS_2 = "/words-2.txt";

		private static final String NAMES_1 = "/names-1.txt.gz";
		private static final String NAMES_2 = "/names-2.txt";

		private static final String ACRONYMS = "/acronyms.txt";

		private static final String GZIP_EXTENSION = ".gz";

		private Map<String, List<String>> map = new HashMap<>();

		private Builder() {
		}

		public Builder addWords() {
			addWords(WORDS_1);
			addWords(WORDS_2);
			addWords(NAMES_1);
			addWords(NAMES_2);
			addWords(ACRONYMS);
			return this;
		}

		public Builder addWords(String resourceName) {
			try (InputStream is = Dictionary.class.getResourceAsStream(resourceName);
					Reader reader = isGzipped(resourceName) ? new InputStreamReader(new GZIPInputStream(is)) : new InputStreamReader(is)) {
				addWords(reader);
			} catch (IOException e) {
				LOG.error("Unable to add words from {}.", resourceName, e);
			}
			return this;
		}

		public Builder addWords(Reader reader) {
			try (BufferedReader bufferedReader = new BufferedReader(reader)) {
				bufferedReader.lines().forEach(s -> addWord(s));
			} catch (IOException e) {
				LOG.error("Unable to add words from Reader.", e);
			}
			return this;
		}

		public Builder addWords(Path path) {
			try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
				lines.forEach(s -> addWord(s));
			} catch (IOException e) {
				LOG.error("Unable to load word list from {}.", path, e);
				throw new IllegalArgumentException("Unable to load word list from Path.", e);
			}
			return this;
		}

		public Builder addWord(String word) {
			// Some lists come with "annotations"
			String cookedWord = removeAnnotation(word).strip();
			List<String> list = map.computeIfAbsent(codeForString(cookedWord), s -> new ArrayList<>());
			if (!list.contains(cookedWord)) {
				list.add(cookedWord);
			}
			return this;
		}

		private static boolean isGzipped(String resourceName) {
			Objects.requireNonNull(resourceName);
			return resourceName.endsWith(GZIP_EXTENSION);
		}

		public Dictionary build() {
			return new Dictionary(map);
		}

		static String removeAnnotation(String raw) {
			Objects.requireNonNull(raw);
			if (raw.isEmpty()) {
				return raw;
			} else {
				int end = raw.length() - 1;
				char c = raw.charAt(end);
				while (c != '.' && !Character.isAlphabetic(c)) {
					c = raw.charAt(--end);
				}
				return raw.substring(0, end + 1);
			}
		}
	}

	int size() {
		return map == null ? 0 : map.size();
	}
}
