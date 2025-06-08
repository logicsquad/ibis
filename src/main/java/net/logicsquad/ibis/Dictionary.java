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
 * Represents a list of known words organised into a {@link Map} keyed by a phonetic encoding.
 * 
 * @author paulh
 * @since 1.0
 */
public class Dictionary {
	private static final Logger LOG = LoggerFactory.getLogger(Dictionary.class);

	private static final int MAX_DISTANCE = 4;

	private final Map<String, List<String>> map = new HashMap<>();

	private final Metaphone codec = new Metaphone();

	public Dictionary() {
		return;
	}

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
