import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Chooses the Project Gutenberg books for the frequency corpus from the Gutenberg catalog, and prints their numbers, one per line. Run
 * with:
 *
 * <pre>
 * java tools/frequency/Select.java pg_catalog.csv &gt; tools/frequency/gutenberg.txt
 * </pre>
 *
 * The catalog is https://www.gutenberg.org/cache/epub/feeds/pg_catalog.csv. The sample is seeded, so the same catalog gives the same
 * books: 150 works of literature (Library of Congress classes PR, PS and PZ), and up to 8 books from each other main class, all in English,
 * leaving out dictionaries, encyclopedias and indexes.
 */
public class Select {
	private static final int LITERATURE = 150;
	private static final int PER_CLASS = 8;
	private static final long SEED = 27;

	public static void main(String[] args) throws IOException {
		List<String> literature = new ArrayList<>();
		Map<Character, List<String>> classes = new TreeMap<>();
		List<List<String>> rows = parse(Files.readString(Path.of(args[0]), StandardCharsets.UTF_8));
		for (List<String> row : rows.subList(1, rows.size())) {
			String number = row.get(0);
			String type = row.get(1);
			String title = row.get(3);
			String language = row.get(4);
			String subjects = row.get(6);
			String locc = row.get(7);
			if (!type.equals("Text") || !language.equals("en") || locc.isBlank() || title.startsWith("Index of")
					|| subjects.matches("(?s).*(Dictionaries|Encyclopedias|Indexes).*")) {
				continue;
			}
			if (locc.matches("(?s).*\\bP[RSZ].*")) {
				literature.add(number);
			} else if (locc.charAt(0) != 'P') {
				classes.computeIfAbsent(locc.charAt(0), c -> new ArrayList<>()).add(number);
			}
		}
		Random random = new Random(SEED);
		List<Integer> chosen = new ArrayList<>(sample(literature, LITERATURE, random));
		classes.values().forEach(books -> chosen.addAll(sample(books, PER_CLASS, random)));
		Collections.sort(chosen);
		chosen.forEach(System.out::println);
		return;
	}

	private static List<Integer> sample(List<String> books, int count, Random random) {
		List<String> shuffled = new ArrayList<>(books);
		Collections.shuffle(shuffled, random);
		return shuffled.subList(0, Math.min(count, shuffled.size())).stream().map(Integer::valueOf).toList();
	}

	// Parses CSV with quoted fields, which may contain commas, doubled quotes and newlines
	private static List<List<String>> parse(String csv) {
		List<List<String>> rows = new ArrayList<>();
		List<String> row = new ArrayList<>();
		StringBuilder field = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < csv.length(); i++) {
			char c = csv.charAt(i);
			if (quoted) {
				if (c == '"' && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
					field.append('"');
					i++;
				} else if (c == '"') {
					quoted = false;
				} else {
					field.append(c);
				}
			} else if (c == '"') {
				quoted = true;
			} else if (c == ',') {
				row.add(field.toString());
				field.setLength(0);
			} else if (c == '\n') {
				row.add(field.toString());
				field.setLength(0);
				rows.add(row);
				row = new ArrayList<>();
			} else if (c != '\r') {
				field.append(c);
			}
		}
		if (!row.isEmpty() || field.length() > 0) {
			row.add(field.toString());
			rows.add(row);
		}
		return rows;
	}
}
