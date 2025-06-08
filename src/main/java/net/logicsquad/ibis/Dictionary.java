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

public class Dictionary {
	private final Map<String, List<String>> map;

	private final Metaphone codec = new Metaphone();

	public Dictionary(Path path) {
		map = new HashMap<>();
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
			System.err.println(e);
		}

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
}
