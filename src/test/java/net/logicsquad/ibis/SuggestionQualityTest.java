package net.logicsquad.ibis;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Measures how well the built-in {@link Dictionary} suggests the intended word for each misspelling in {@code /misspellings.txt}, and
 * fails if that gets worse.
 *
 * @author paulh
 */
public class SuggestionQualityTest {
	private static final Logger LOG = LoggerFactory.getLogger(SuggestionQualityTest.class);

	private static final String MISSPELLINGS = "/misspellings.txt";

	// Minimum percentages of misspellings whose intended word is suggested first, in the top 3, and in the top 10. Raise these when
	// suggestions improve.
	private static final int MIN_FIRST = 94;
	private static final int MIN_TOP_3 = 97;
	private static final int MIN_TOP_10 = 97;

	// https://github.com/logicsquad/ibis/issues/27
	@Test
	public void suggestionsRankIntendedWordsHighly() throws IOException {
		Dictionary dictionary = Dictionary.builtIn(Locale.ENGLISH);
		int count = 0;
		int first = 0;
		int top3 = 0;
		int top10 = 0;
		List<String> misses = new ArrayList<>();
		for (String[] pair : pairs()) {
			int rank = dictionary.suggestionsFor(pair[0]).indexOf(pair[1]) + 1;
			count++;
			first += rank == 1 ? 1 : 0;
			top3 += rank >= 1 && rank <= 3 ? 1 : 0;
			top10 += rank >= 1 ? 1 : 0;
			if (rank != 1) {
				misses.add(pair[0] + " (" + pair[1] + (rank == 0 ? ", not suggested)" : ", " + rank + ")"));
			}
		}
		String report = "%d misspellings: intended word first %d%%, top 3 %d%%, top 10 %d%%".formatted(count, percent(first, count),
				percent(top3, count), percent(top10, count));
		LOG.info("{}. Not first: {}", report, String.join(", ", misses));
		assertTrue(percent(first, count) >= MIN_FIRST, report);
		assertTrue(percent(top3, count) >= MIN_TOP_3, report);
		assertTrue(percent(top10, count) >= MIN_TOP_10, report);
		return;
	}

	// Returns pairs of misspelling and intended word
	private static List<String[]> pairs() throws IOException {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(SuggestionQualityTest.class.getResourceAsStream(MISSPELLINGS), StandardCharsets.UTF_8))) {
			return reader.lines().filter(l -> !l.isBlank() && !l.startsWith("#")).map(l -> l.split(" ")).toList();
		}
	}

	// Returns part as a whole percentage of whole, rounded down
	private static int percent(int part, int whole) {
		return 100 * part / whole;
	}
}
