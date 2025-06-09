package net.logicsquad.ibis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
	 * Maximum <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">Levenshtein distance</a> between an incorrect word and suggestions
	 * returned
	 */
	private static final int MAX_DISTANCE = 4;

	/**
	 * Map for words keyed on phonetic representation
	 */
	final Map<String, List<String>> map = new HashMap<>();

	/**
	 * Codec for providing phonetic representations
	 */
	private final StringEncoder codec = new Metaphone();

	/**
	 * Constructor providing no initial words
	 */
	public Dictionary() {
		return;
	}

	/**
	 * Constructor taking a {@link Path} to a list of words
	 * 
	 * @param path {@link Path} to a list of words
	 */
	public Dictionary(Path path) {
		try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
			lines.forEach(s -> {
				addWord(s);
			});
		} catch (IOException e) {
			LOG.error("Unable to load world list from {}.", path, e);
			throw new IllegalArgumentException("Unable to load word list from Path.", e);
		}
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
	private String codeForString(String word) {
		try {
			return codec.encode(word);
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
	private String codeForWord(Word word) {
		return codeForString(word.text());
	}
}
