package net.logicsquad.ibis;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoder;
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
 * A {@code Dictionary} has a {@link Locale}, which selects its phonetic encoder, its built-in word lists, and the {@link Tokenizer} that a
 * {@link Checker} uses with it. A {@code Dictionary} is immutable and thread-safe, so a single instance can be shared between threads.
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
	 * Locale of words in this {@code Dictionary}
	 */
	private final Locale locale;

	/**
	 * Codec for providing phonetic representations
	 */
	private final StringEncoder codec;

	/**
	 * Maximum edit distance between an incorrect word and suggestions that sound like it
	 */
	private static final int MAX_DISTANCE = 3;

	/**
	 * Maximum number of suggestions returned by {@link #suggestionsFor(String)}
	 */
	static final int MAX_SUGGESTIONS = 10;

	/**
	 * Weight of the natural logarithm of a suggestion's rank in its score, so that one more edit costs as much as being about 800 times less
	 * common
	 */
	private static final double RANK_WEIGHT = 0.15;

	/**
	 * Amount taken off the score of a suggestion that sounds like the incorrect word
	 */
	private static final double SOUNDS_LIKE_BONUS = 0.4;

	/**
	 * Map for words keyed on phonetic representation
	 */
	private final Map<String, List<String>> map;

	/**
	 * Optional {@code Dictionary} whose words this one shares rather than copies
	 */
	private final Dictionary base;

	/**
	 * Characters appearing in lower case words in this {@code Dictionary}, including those in {@link #base}, for generating edits of
	 * incorrect words
	 */
	private final char[] alphabet;

	/**
	 * Ranks of words in lower case, from 1 for the most common, for ranking suggestions
	 */
	private final Map<String, Integer> ranks;

	/**
	 * Built-in {@code Dictionary}s, built on first use
	 */
	private static final Map<Language, Dictionary> BUILT_IN = new ConcurrentHashMap<>();

	/**
	 * Ranks of words in each {@link Language}, loaded on first use
	 */
	private static final Map<Language, Map<String, Integer>> RANKS = new ConcurrentHashMap<>();

	/**
	 * Constructor
	 * 
	 * @param locale a {@link Locale}
	 * @param codec  a {@link StringEncoder}
	 * @param map    map from phonetic representations to word lists
	 * @param base   optional {@code Dictionary} whose words this one shares
	 */
	private Dictionary(Locale locale, StringEncoder codec, Map<String, List<String>> map, Dictionary base) {
		this.locale = locale;
		this.codec = codec;
		this.map = map;
		this.base = base;
		ranks = RANKS.computeIfAbsent(Language.of(locale), Dictionary::loadRanks);
		Set<Character> characters = new TreeSet<>();
		if (base != null) {
			for (char c : base.alphabet) {
				characters.add(c);
			}
		}
		map.values().forEach(words -> words.forEach(w -> toLowerCase(w, locale).chars().forEach(c -> characters.add((char) c))));
		alphabet = new char[characters.size()];
		int i = 0;
		for (char c : characters) {
			alphabet[i++] = c;
		}
		return;
	}

	/**
	 * Returns the shared {@code Dictionary} of built-in words for the language of {@code locale}, building it on first use. Its
	 * {@link #locale()} is the language alone, such as {@link Locale#ENGLISH} for {@link Locale#UK}.
	 * 
	 * @param locale a {@link Locale}
	 * @return built-in {@code Dictionary}
	 * @throws NullPointerException     if {@code locale} is {@code null}
	 * @throws IllegalArgumentException if the language of {@code locale} is not supported
	 * @since 0.4
	 */
	public static Dictionary builtIn(Locale locale) {
		return BUILT_IN.computeIfAbsent(Language.of(locale), language -> new Builder(language.locale()).loadBuiltInWords().build());
	}

	/**
	 * Returns a new {@code Dictionary} containing {@code words} as well as the words in this one, which it shares rather than copies.
	 * 
	 * @param words a {@link Collection} of words
	 * @return new {@code Dictionary}
	 * @throws NullPointerException if {@code words} is {@code null}, or contains {@code null}
	 * @since 0.4
	 */
	public Dictionary with(Collection<String> words) {
		return new Builder(locale, this).addWords(words).build();
	}

	/**
	 * Returns the {@link Locale} of words in this {@code Dictionary}.
	 * 
	 * @return {@link Locale}
	 * @since 0.4
	 */
	public Locale locale() {
		return locale;
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
		List<String> words = wordsFor(codeForString(codec, word));
		return (words.contains(word) || words.contains(toLowerCase(word, locale)) || words.contains(toInitialCap(word, locale)));
	}

	/**
	 * Returns up to 10 suggestions for {@code word}, best first, or an empty list if {@code word} is spelled correctly. See
	 * {@link #suggestionsFor(String, int)}.
	 * 
	 * @param word a word
	 * @return a list of suggestions
	 * @throws NullPointerException if {@code word} is {@code null}
	 */
	public List<String> suggestionsFor(String word) {
		return suggestionsFor(word, MAX_SUGGESTIONS);
	}

	/**
	 * <p>
	 * Returns up to {@code limit} suggestions for {@code word}, best first, or an empty list if {@code word} is spelled correctly.
	 * </p>
	 * 
	 * <p>
	 * Suggestions are the words in this {@code Dictionary} that are within an edit distance of 1 of {@code word}, or that sound like it
	 * (they have the same phonetic code) and are within an edit distance of 3. The edit distance counts an insertion, deletion, substitution
	 * or transposition of adjacent characters as one edit, and ignores case. Suggestions are ordered by a score: the edit distance, plus a
	 * little for how rare the suggestion is in this {@code Dictionary}'s language, less a little if it sounds like {@code word}. So closer
	 * words usually come first, but a much more common word can come before a closer, rarer one. Words with the same score are ordered
	 * alphabetically. Words differing only in case are suggested once, in lower case if this {@code Dictionary} contains that form.
	 * </p>
	 * 
	 * @param word  a word
	 * @param limit maximum number of suggestions
	 * @return a list of suggestions
	 * @throws NullPointerException     if {@code word} is {@code null}
	 * @throws IllegalArgumentException if {@code limit} is negative
	 * @since 0.5
	 */
	public List<String> suggestionsFor(String word, int limit) {
		Objects.requireNonNull(word);
		if (limit < 0) {
			throw new IllegalArgumentException("limit must not be negative.");
		}
		return isCorrect(word) ? List.of() : suggestionsForIncorrect(word, limit);
	}

	/**
	 * Returns up to {@code limit} suggestions for {@code word}, which the caller already knows to be spelled incorrectly, as
	 * {@link #suggestionsFor(String, int)} does.
	 * 
	 * @param word  an incorrect word
	 * @param limit maximum number of suggestions
	 * @return a list of suggestions
	 */
	List<String> suggestionsForIncorrect(String word, int limit) {
		String lowerCaseWord = toLowerCase(word, locale);
		// Keyed on lower case, so that words differing only in case are suggested once
		Map<String, Suggestion> suggestions = new LinkedHashMap<>();
		for (String s : wordsFor(codeForString(codec, word))) {
			String lowerCase = toLowerCase(s, locale);
			int distance = Distance.between(lowerCaseWord, lowerCase);
			if (distance <= MAX_DISTANCE) {
				suggestions.merge(lowerCase, new Suggestion(s, lowerCase, distance, true, rankOf(lowerCase)), Suggestion::preferLowerCase);
			}
		}
		for (String edit : editsOf(lowerCaseWord)) {
			for (String s : wordsFor(codeForString(codec, edit))) {
				String lowerCase = toLowerCase(s, locale);
				if (lowerCase.equals(edit)) {
					suggestions.merge(lowerCase, new Suggestion(s, lowerCase, 1, false, rankOf(lowerCase)), Suggestion::preferLowerCase);
				}
			}
		}
		return suggestions.values().stream().sorted(Suggestion.ORDER).limit(limit).map(Suggestion::word).toList();
	}

	/**
	 * Returns the rank of {@code lowerCaseWord} in this {@code Dictionary}'s language, from 1 for the most common, or one more than the
	 * least common if it isn't in the frequency list.
	 * 
	 * @param lowerCaseWord a word in lower case
	 * @return rank of {@code lowerCaseWord}
	 */
	private int rankOf(String lowerCaseWord) {
		return ranks.getOrDefault(lowerCaseWord, ranks.size() + 1);
	}

	/**
	 * Returns every string one edit from {@code word}: each deletion, transposition of adjacent characters, substitution and insertion of a
	 * character, using the characters in {@link #alphabet}.
	 * 
	 * @param word a word
	 * @return strings one edit from {@code word}
	 */
	private Set<String> editsOf(String word) {
		Set<String> edits = new LinkedHashSet<>();
		int length = word.length();
		for (int i = 0; i < length; i++) {
			edits.add(word.substring(0, i) + word.substring(i + 1));
		}
		for (int i = 0; i < length - 1; i++) {
			edits.add(word.substring(0, i) + word.charAt(i + 1) + word.charAt(i) + word.substring(i + 2));
		}
		for (int i = 0; i <= length; i++) {
			for (char c : alphabet) {
				if (i < length) {
					edits.add(word.substring(0, i) + c + word.substring(i + 1));
				}
				edits.add(word.substring(0, i) + c + word.substring(i));
			}
		}
		edits.remove(word);
		edits.remove("");
		return edits;
	}

	/**
	 * A candidate suggestion, with what's needed to rank it.
	 * 
	 * @param word          a word from this {@code Dictionary}
	 * @param lowerCaseWord {@code word} in lower case
	 * @param distance      edit distance from the incorrect word, ignoring case
	 * @param soundsLike    whether {@code word} has the same phonetic code as the incorrect word
	 * @param rank          rank of {@code word} by how common it is, from 1 for the most common
	 */
	private record Suggestion(String word, String lowerCaseWord, int distance, boolean soundsLike, int rank) {
		/**
		 * Best first: by score, then alphabetically ignoring case
		 */
		private static final Comparator<Suggestion> ORDER = Comparator.comparingDouble(Suggestion::score)
				.thenComparing(Suggestion::lowerCaseWord);

		/**
		 * Returns this suggestion's score, lowest best: its edit distance, plus {@link #RANK_WEIGHT} times the natural logarithm of its rank,
		 * less {@link #SOUNDS_LIKE_BONUS} if it sounds like the incorrect word. A word's probability is roughly proportional to the inverse of
		 * its rank (Zipf's law), so this weighs how close a suggestion is against how unlikely it is.
		 * 
		 * @return score
		 */
		private double score() {
			return distance + RANK_WEIGHT * Math.log(rank) - (soundsLike ? SOUNDS_LIKE_BONUS : 0);
		}

		/**
		 * Returns this {@code Suggestion} or {@code other}, which differ only in the case of their words, preferring the one whose word is
		 * lower case: {@link #isCorrect(String)} accepts a lower case word with an initial capital too, but not the reverse.
		 * 
		 * @param other a {@code Suggestion} whose word differs from this one's only in case
		 * @return {@code Suggestion} to keep
		 */
		private Suggestion preferLowerCase(Suggestion other) {
			Suggestion kept = other.word.equals(other.lowerCaseWord) && !word.equals(lowerCaseWord) ? other : this;
			return new Suggestion(kept.word, lowerCaseWord, Math.min(distance, other.distance), soundsLike || other.soundsLike, rank);
		}
	}

	/**
	 * Returns the words with phonetic representation {@code code}, including those in {@link #base}.
	 * 
	 * @param code a phonetic representation
	 * @return list of words
	 */
	private List<String> wordsFor(String code) {
		List<String> words = map.getOrDefault(code, List.of());
		if (base == null) {
			return words;
		}
		List<String> baseWords = base.wordsFor(code);
		if (words.isEmpty()) {
			return baseWords;
		} else if (baseWords.isEmpty()) {
			return words;
		} else {
			return Stream.concat(baseWords.stream(), words.stream()).distinct().toList();
		}
	}

	/**
	 * Returns the ranks of words in {@code language}'s frequency list, from 1 for the most common.
	 * 
	 * @param language a {@link Language}
	 * @return map from words in lower case to their ranks
	 * @throws UncheckedIOException if the frequency list cannot be found or read
	 */
	private static Map<String, Integer> loadRanks(Language language) {
		String name = language.frequencyList();
		try (InputStream is = Dictionary.class.getResourceAsStream(name)) {
			if (is == null) {
				throw new FileNotFoundException("Unable to find resource " + name + ".");
			}
			Map<String, Integer> ranks = new HashMap<>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(is), StandardCharsets.UTF_8));
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				if (!line.isBlank()) {
					ranks.putIfAbsent(line.strip(), ranks.size() + 1);
				}
			}
			return Map.copyOf(ranks);
		} catch (IOException e) {
			LOG.error("Unable to load word frequencies from {}.", name, e);
			throw new UncheckedIOException("Unable to load word frequencies from " + name + ".", e);
		}
	}

	/**
	 * Returns {@code word} as lower case, using the rules of {@code locale} rather than the default {@link Locale}.
	 *
	 * @param word   a word
	 * @param locale a {@link Locale}
	 * @return {@code word} as lower case
	 */
	static String toLowerCase(String word, Locale locale) {
		return word.toLowerCase(locale);
	}

	/**
	 * Returns {@code word} with first character upper case, and remainder lower case, using the rules of {@code locale} rather than the
	 * default {@link Locale}. If {@code word} is empty, returns it unchanged.
	 *
	 * @param word   a word
	 * @param locale a {@link Locale}
	 * @return {@code word} with initial cap
	 */
	static String toInitialCap(String word, Locale locale) {
		return word.isEmpty() ? word : word.substring(0, 1).toUpperCase(locale) + word.substring(1).toLowerCase(locale);
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
		 * First two bytes of a stream compressed with GZip
		 */
		private static final int GZIP_MAGIC = GZIPInputStream.GZIP_MAGIC;

		/**
		 * Map from phonetic codes to lists of words
		 */
		private final Map<String, List<String>> map = new HashMap<>();

		/**
		 * Locale for {@code Dictionary}
		 */
		private final Locale locale;

		/**
		 * {@link Language} of {@link #locale}
		 */
		private final Language language;

		/**
		 * {@link StringEncoder} suitable for locale
		 */
		private final StringEncoder codec;

		/**
		 * Optional {@code Dictionary} whose words the new {@code Dictionary} shares
		 */
		private Dictionary base;

		/**
		 * Constructor taking a {@link Locale}
		 * 
		 * @since 0.2
		 */
		private Builder(Locale locale) {
			this(locale, null);
			return;
		}

		/**
		 * Constructor taking a {@link Locale} and a {@code Dictionary} to share words with
		 * 
		 * @param locale a {@link Locale}
		 * @param base   optional {@code Dictionary} whose words the new {@code Dictionary} shares
		 */
		private Builder(Locale locale, Dictionary base) {
			language = Language.of(locale);
			this.locale = locale;
			this.base = base;
			codec = language.encoder();
			return;
		}

		/**
		 * Adds words from all built-in word lists for the language of {@link #locale}. The new {@code Dictionary} shares them with
		 * {@link Dictionary#builtIn(Locale)} rather than copying them.
		 *
		 * @return this object
		 * @since 0.4
		 */
		public Builder addBuiltInWords() {
			if (base == null) {
				base = Dictionary.builtIn(locale);
			} else {
				loadBuiltInWords();
			}
			return this;
		}

		/**
		 * Adds copies of words from all built-in word lists for the language of {@link #locale}.
		 *
		 * @return this object
		 */
		private Builder loadBuiltInWords() {
			language.builtInWordLists().forEach(name -> addWords(Dictionary.class, name));
			return this;
		}

		/**
		 * Adds words from the resource {@code resourceName}, found through {@code context} with {@link Class#getResourceAsStream(String)}, so
		 * that a name starting with {@code /} is absolute, and any other name is relative to the package of {@code context}. The resource must
		 * be encoded in UTF-8, and is decompressed if it is compressed with GZip.
		 *
		 * @param context      a {@link Class} whose class loader and package are used to find the resource
		 * @param resourceName name of a resource
		 * @return this object
		 * @throws NullPointerException if either argument is {@code null}
		 * @throws UncheckedIOException if the resource cannot be found or read
		 * @since 0.4
		 */
		public Builder addWords(Class<?> context, String resourceName) {
			Objects.requireNonNull(context);
			Objects.requireNonNull(resourceName);
			LOG.debug("Adding words from resource '{}'...", resourceName);
			try (InputStream is = context.getResourceAsStream(resourceName)) {
				if (is == null) {
					throw new FileNotFoundException("Unable to find resource " + resourceName + ".");
				}
				return readWords(is);
			} catch (IOException | UncheckedIOException e) {
				LOG.error("Unable to add words from {}.", resourceName, e);
				throw new UncheckedIOException("Unable to add words from " + resourceName + ".", unwrap(e));
			}
		}

		/**
		 * Adds words from file at {@code path}. The file must be encoded in UTF-8, and is decompressed if it is compressed with GZip.
		 *
		 * @param path a {@link Path}
		 * @return this object
		 * @throws NullPointerException if {@code path} is {@code null}
		 * @throws UncheckedIOException if the file cannot be read
		 */
		public Builder addWords(Path path) {
			Objects.requireNonNull(path);
			LOG.debug("Adding words from path '{}'...", path);
			try (InputStream is = Files.newInputStream(path)) {
				return readWords(is);
			} catch (IOException | UncheckedIOException e) {
				LOG.error("Unable to add words from {}.", path, e);
				throw new UncheckedIOException("Unable to add words from " + path + ".", unwrap(e));
			}
		}

		/**
		 * Adds words from {@code is}, which must contain text encoded in UTF-8, and is decompressed if it is compressed with GZip. The caller
		 * remains responsible for closing {@code is}.
		 *
		 * @param is an {@link InputStream}
		 * @return this object
		 * @throws NullPointerException if {@code is} is {@code null}
		 * @throws UncheckedIOException if {@code is} cannot be read
		 * @since 0.4
		 */
		public Builder addWords(InputStream is) {
			Objects.requireNonNull(is);
			try {
				return readWords(is);
			} catch (IOException | UncheckedIOException e) {
				LOG.error("Unable to add words from InputStream.", e);
				throw new UncheckedIOException("Unable to add words from InputStream.", unwrap(e));
			}
		}

		/**
		 * Adds words from {@code reader}, one per line. The caller remains responsible for closing {@code reader}.
		 *
		 * @param reader a {@link Reader}
		 * @return this object
		 * @throws NullPointerException if {@code reader} is {@code null}
		 * @throws UncheckedIOException if {@code reader} cannot be read
		 */
		public Builder addWords(Reader reader) {
			Objects.requireNonNull(reader);
			try {
				// Not closed, because closing the BufferedReader would close reader
				new BufferedReader(reader).lines().forEach(this::addWord);
			} catch (UncheckedIOException e) {
				LOG.error("Unable to add words from Reader.", e);
				throw new UncheckedIOException("Unable to add words from Reader.", e.getCause());
			}
			return this;
		}

		/**
		 * Adds each of {@code words}, as {@link #addWord(String)} does.
		 *
		 * @param words a {@link Collection} of words
		 * @return this object
		 * @throws NullPointerException if {@code words} is {@code null}, or contains {@code null}
		 * @since 0.4
		 */
		public Builder addWords(Collection<String> words) {
			Objects.requireNonNull(words);
			words.forEach(this::addWord);
			return this;
		}

		/**
		 * Adds words from {@code is}, without closing it. {@code is} must contain text encoded in UTF-8, and is decompressed if it starts with
		 * the GZip magic number.
		 *
		 * @param is an {@link InputStream}
		 * @return this object
		 * @throws IOException          if {@code is} cannot be read
		 * @throws UncheckedIOException if {@code is} cannot be read, or does not contain valid UTF-8
		 */
		private Builder readWords(InputStream is) throws IOException {
			InputStream in = new BufferedInputStream(is);
			in.mark(2);
			int magic = in.read() | (in.read() << 8);
			in.reset();
			// Neither GZIPInputStream nor the readers below are closed, because closing them would close is
			Reader reader = new InputStreamReader(magic == GZIP_MAGIC ? new GZIPInputStream(in) : in, StandardCharsets.UTF_8.newDecoder());
			new BufferedReader(reader).lines().forEach(this::addWord);
			return this;
		}

		/**
		 * Returns the {@link IOException} behind {@code e}.
		 *
		 * @param e an {@link IOException} or {@link UncheckedIOException}
		 * @return {@code e}, or the cause of an {@link UncheckedIOException}
		 */
		private static IOException unwrap(Exception e) {
			return e instanceof UncheckedIOException u ? u.getCause() : (IOException) e;
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
		 * Creates and returns a new {@code Dictionary} from this {@code Builder}. The new {@code Dictionary} holds a copy of the words added so
		 * far, so adding words to this {@code Builder} afterwards does not change it.
		 * 
		 * @return new {@code Dictionary}
		 */
		public Dictionary build() {
			Map<String, List<String>> copy = HashMap.newHashMap(map.size());
			map.forEach((code, words) -> copy.put(code, List.copyOf(words)));
			return new Dictionary(locale, codec, Collections.unmodifiableMap(copy), base);
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
