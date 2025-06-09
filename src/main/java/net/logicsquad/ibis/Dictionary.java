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
	private final Map<String, List<String>> map = new HashMap<>();

	/**
	 * Codec for providing phonetic representations
	 */
	private final Metaphone codec = new Metaphone();

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
				String m = codec.metaphone(s);
				if (!map.containsKey(m)) {
					ArrayList<String> l = new ArrayList<>();
					l.add(s);
					map.put(m, l);
				} else {
					map.get(m).add(s);
				}
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
		String m = codec.metaphone(word);
		if (!map.containsKey(m)) {
			ArrayList<String> l = new ArrayList<>();
			l.add(word);
			map.put(m, l);
		} else {
			map.get(m).add(word);
		}
	}

	/**
	 * Does {@code word} contain a word that is spelled correctly?
	 * 
	 * @param word a {@link Word}
	 * @return {@code true} if {@code word} contains a word that is spelled correctly, otherwise {@code false}
	 */
	public boolean isCorrect(Word word) {
		String m = codec.metaphone(word.text());
		if (map.containsKey(m)) {
			if (map.get(m).contains(word.text())) {
				return true;
			} else {
				return map.get(m).contains(word.text().toLowerCase());
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
		String m = codec.metaphone(word.text());
		if (map.containsKey(m)) {
			List<String> result = new ArrayList<>();
			for (String s : map.get(m)) {
				if (LevenshteinDistance.getDefaultInstance().apply(word.text(), s) < MAX_DISTANCE) {
					result.add(s);
				}
			}
			return result;
		} else {
			return List.of();
		}
	}
}
