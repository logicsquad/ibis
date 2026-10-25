package net.logicsquad.ibis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
 * <p>
 * A {@code Dictionary} is immutable and thread-safe, so a single instance can be shared between threads.
 * </p>
 * 
 * @author paulh
 * @since 0.1
 */
public class Dictionary {
	/**
	 * Logger
	 */
	private static final Logger LOG = LoggerFactory.getLogger(Dictionary.class);

	/**
	 * Codec for providing phonetic representations
	 */
	private final StringEncoder codec;

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
	 * @param codec a {@link StringEncoder}
	 * @param map map from phonetic representations to word lists
	 */
	private Dictionary(StringEncoder codec, Map<String, List<String>> map) {
		this.codec = codec;
		this.map = map;
		return;
	}

	/**
	 * Is {@code word} spelled correctly? A word is correct if this {@code Dictionary} contains it verbatim, in lower case, or with an initial
	 * capital.
	 * 
	 * @param word a word
	 * @return {@code true} if {@code word} is spelled correctly, otherwise {@code false}
	 * @throws NullPointerException if {@code word} is {@code null}
	 */
	public boolean isCorrect(String word) {
		Objects.requireNonNull(word);
		List<String> words = map.get(codeForString(codec, word));
		return words != null && (words.contains(word) || words.contains(toLowerCase(word)) || words.contains(toInitialCap(word)));
	}

	/**
	 * Returns a list of suggestions for {@code word}, or an empty list if {@code word} is spelled correctly.
	 * 
	 * @param word a word
	 * @return a list of suggestions
	 * @throws NullPointerException if {@code word} is {@code null}
	 */
	public List<String> suggestionsFor(String word) {
		Objects.requireNonNull(word);
		return isCorrect(word) ? List.of() : suggestionsForIncorrect(word);
	}

	/**
	 * Returns a list of suggestions for {@code word}, which the caller already knows to be spelled incorrectly.
	 * 
	 * @param word an incorrect word
	 * @return a list of suggestions
	 */
	List<String> suggestionsForIncorrect(String word) {
		List<String> words = map.getOrDefault(codeForString(codec, word), List.of());
		return words.stream().filter(s -> LevenshteinDistance.getDefaultInstance().apply(word, s) < MAX_DISTANCE).toList();
	}

	/**
	 * Returns {@code word} as lower case, using the rules of {@link Locale#ROOT} rather than the default {@link Locale}.
	 *
	 * @param word a word
	 * @return {@code word} as lower case
	 */
	static String toLowerCase(String word) {
		return word.toLowerCase(Locale.ROOT);
	}

	/**
	 * Returns {@code word} with first character upper case, and remainder lower case, using the rules of {@link Locale#ROOT} rather than the
	 * default {@link Locale}. If {@code word} is empty, returns it unchanged.
	 *
	 * @param word a word
	 * @return {@code word} with initial cap
	 */
	static String toInitialCap(String word) {
		return word.isEmpty() ? word : word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT);
	}

	/**
	 * Returns a code for {@code word} using {@code codec}.
	 * 
	 * @param codec a {@link StringEncoder}
	 * @param word a word
	 * @return code for {@code word}
	 */
	private static String codeForString(StringEncoder codec, String word) {
		try {
			return codec.encode(word);
		} catch (EncoderException e) {
			LOG.error("Unable to encode '{}'.", word, e);
			return null;
		}
	}

	/**
	 * Returns a new {@code Builder} for {@code Locale.ENGLISH}.
	 *
	 * @return a new {@code Builder}
	 */
	public static Builder builder() {
		return builder(Locale.ENGLISH);
	}

	/**
	 * Returns a new {@code Builder}.
	 *
	 * @param locale a {@link Locale}
	 * @return a new {@code Builder}
	 * @throws NullPointerException     if {@code locale} is {@code null}
	 * @throws IllegalArgumentException if the language of {@code locale} is not supported
	 * @since 0.2
	 */
	public static Builder builder(Locale locale) {
		return new Builder(locale);
	}

	/**
	 * Builder for {@code Dictionary} objects.
	 */
	public static class Builder {
		/**
		 * World list 1
		 */
		private static final String WORDS_1 = "/en/words-1.txt.gz";

		/**
		 * World list 2
		 */
		private static final String WORDS_2 = "/en/words-2.txt";

		/**
		 * Names list 1
		 */
		private static final String NAMES_1 = "/en/names-1.txt.gz";

		/**
		 * Names list 2
		 */
		private static final String NAMES_2 = "/en/names-2.txt";

		/**
		 * Acronyms list
		 */
		private static final String ACRONYMS = "/en/acronyms.txt";

		/**
		 * Extension for compressed lists
		 */
		private static final String GZIP_EXTENSION = ".gz";

		/**
		 * Map from phonetic codes to lists of words
		 */
		private final Map<String, List<String>> map = new HashMap<>();

		/**
		 * Locale for {@code Dictionary}
		 */
		private final Locale locale;

		/**
		 * {@link StringEncoder} suitable for locale
		 */
		private final StringEncoder codec;

		/**
		 * Constructor taking a {@link Locale}
		 * 
		 * @since 0.2
		 */
		private Builder(Locale locale) {
			this.locale = locale;
			codec = switch (locale.getLanguage()) {
			case "en" -> new Metaphone();
			default -> throw new IllegalArgumentException("Unknown Locale: " + locale);
			};
			return;
		}

		/**
		 * Adds words from all built-in word lists for the language of {@link #locale}.
		 *
		 * @return this object
		 */
		public Builder addWords() {
			if ("en".equals(locale.getLanguage())) {
				addWords(WORDS_1);
				addWords(WORDS_2);
				addWords(NAMES_1);
				addWords(NAMES_2);
				addWords(ACRONYMS);
			}
			return this;
		}

		/**
		 * Adds words from {@code resourceName} on classpath. The resource must be encoded in UTF-8, and is decompressed with GZip if its name
		 * ends with {@code .gz}.
		 *
		 * @param resourceName name of a resource on classpath
		 * @return this object
		 * @throws NullPointerException     if {@code resourceName} is {@code null}
		 * @throws IllegalArgumentException if the resource cannot be found or read
		 */
		public Builder addWords(String resourceName) {
			Objects.requireNonNull(resourceName);
			LOG.debug("Adding words from resource '{}'...", resourceName);
			InputStream is = Dictionary.class.getResourceAsStream(resourceName);
			if (is == null) {
				LOG.error("Unable to find resource {}.", resourceName);
				throw new IllegalArgumentException("Unable to find resource " + resourceName + ".");
			}
			return readWords(is, resourceName);
		}

		/**
		 * Adds words from {@code reader}, then closes it.
		 *
		 * @param reader a {@link Reader}
		 * @return this object
		 * @throws NullPointerException     if {@code reader} is {@code null}
		 * @throws IllegalArgumentException if {@code reader} cannot be read
		 */
		public Builder addWords(Reader reader) {
			Objects.requireNonNull(reader);
			try (BufferedReader bufferedReader = new BufferedReader(reader)) {
				bufferedReader.lines().forEach(this::addWord);
			} catch (IOException | UncheckedIOException e) {
				LOG.error("Unable to add words from Reader.", e);
				throw new IllegalArgumentException("Unable to add words from Reader.", e);
			}
			return this;
		}

		/**
		 * Adds words from file at {@code path}. The file must be encoded in UTF-8, and is decompressed with GZip if its name ends with
		 * {@code .gz}.
		 *
		 * @param path a {@link Path}
		 * @return this object
		 * @throws NullPointerException     if {@code path} is {@code null}
		 * @throws IllegalArgumentException if the file cannot be read
		 */
		public Builder addWords(Path path) {
			Objects.requireNonNull(path);
			LOG.debug("Adding words from path '{}'...", path);
			InputStream is;
			try {
				is = Files.newInputStream(path);
			} catch (IOException e) {
				LOG.error("Unable to add words from {}.", path, e);
				throw new IllegalArgumentException("Unable to add words from " + path + ".", e);
			}
			return readWords(is, path.toString());
		}

		/**
		 * Adds words from {@code is}, then closes it. {@code is} must contain text encoded in UTF-8, and is decompressed with GZip if
		 * {@code name} ends with {@code .gz}.
		 *
		 * @param is   an {@link InputStream}
		 * @param name name of the word list {@code is} reads
		 * @return this object
		 * @throws IllegalArgumentException if {@code is} cannot be read, or does not contain valid UTF-8
		 */
		private Builder readWords(InputStream is, String name) {
			try (InputStream in = is;
					BufferedReader reader = new BufferedReader(new InputStreamReader(isGzipped(name) ? new GZIPInputStream(in) : in, StandardCharsets.UTF_8.newDecoder()))) {
				reader.lines().forEach(this::addWord);
			} catch (IOException | UncheckedIOException e) {
				LOG.error("Unable to add words from {}.", name, e);
				throw new IllegalArgumentException("Unable to add words from " + name + ".", e);
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
			List<String> list = map.computeIfAbsent(codeForString(codec, cookedWord), s -> new ArrayList<>());
			if (!list.contains(cookedWord)) {
				list.add(cookedWord);
			} else {
				LOG.debug("'{}' is already in this Dictionary.", cookedWord);
			}
			return this;
		}

		/**
		 * Does {@code name} represent a file compressed with GZip?
		 *
		 * @param name name of a resource or file
		 * @return {@code true} if it looks like the file is compressed with GZip, otherwise {@code false}
		 */
		private static boolean isGzipped(String name) {
			Objects.requireNonNull(name);
			return name.endsWith(GZIP_EXTENSION);
		}

		/**
		 * Creates and returns a new {@code Dictionary} from this {@code Builder}. The new {@code Dictionary} holds a copy of the words added so
		 * far, so adding words to this {@code Builder} afterwards does not change it.
		 * 
		 * @return new {@code Dictionary}
		 */
		public Dictionary build() {
			Map<String, List<String>> copy = HashMap.newHashMap(map.size());
			map.forEach((code, words) -> copy.put(code, List.copyOf(words)));
			return new Dictionary(codec, Collections.unmodifiableMap(copy));
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
