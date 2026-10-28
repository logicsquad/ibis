import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import net.logicsquad.ibis.Dictionary;
import net.logicsquad.ibis.Tokenizer;

/**
 * Counts words in the frequency corpus that {@link Fetch} downloads, and writes the built-in dictionary's words, most common first, in
 * lower case, one per line, compressed with GZip. Run with:
 *
 * <pre>
 * mvn -pl ibis compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
 * java -cp ibis/target/classes:$(cat ibis/target/classpath.txt) tools/frequency/Count.java &lt;corpus directory&gt; ibis/src/main/resources/en/frequency.txt.gz
 * </pre>
 *
 * Each subdirectory of the corpus directory is a source. Words are found with Ibis's own {@link Tokenizer}, so they are counted as Ibis
 * checks them, and counted ignoring case. A word's frequency is its share of each source's words, averaged over the sources, so each
 * source counts equally, whatever its size. Words seen fewer than {@link #MIN_COUNT} times in all, or not in the built-in dictionary, are
 * left out, and at most {@link #MAX_WORDS} are written.
 */
public class Count {
	private static final int MIN_COUNT = 3;
	private static final int MAX_WORDS = 30_000;

	public static void main(String[] args) throws IOException {
		Tokenizer tokenizer = Tokenizer.forLocale(Locale.ENGLISH);
		Dictionary dictionary = Dictionary.builtIn(Locale.ENGLISH);
		Map<String, Double> frequencies = new HashMap<>();
		Map<String, Integer> totals = new HashMap<>();
		List<Path> sources;
		try (Stream<Path> paths = Files.list(Path.of(args[0]))) {
			sources = paths.filter(Files::isDirectory).sorted().toList();
		}
		for (Path source : sources) {
			Map<String, Integer> counts = new HashMap<>();
			long words = 0;
			List<Path> files;
			try (Stream<Path> paths = Files.list(source)) {
				files = paths.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
			}
			for (Path file : files) {
				for (String word : tokenizer.tokenize(Files.readString(file, StandardCharsets.UTF_8)).map(w -> w.text().toLowerCase(Locale.ENGLISH)).toList()) {
					counts.merge(word, 1, Integer::sum);
					words++;
				}
			}
			System.err.printf("%s: %d files, %d words, %d distinct%n", source.getFileName(), files.size(), words, counts.size());
			for (var entry : counts.entrySet()) {
				frequencies.merge(entry.getKey(), (double) entry.getValue() / words / sources.size(), Double::sum);
				totals.merge(entry.getKey(), entry.getValue(), Integer::sum);
			}
		}
		List<String> ranked = new ArrayList<>();
		for (String word : frequencies.keySet()) {
			if (totals.get(word) >= MIN_COUNT && (dictionary.isCorrect(word) || dictionary.isCorrect(word.toUpperCase(Locale.ENGLISH)))) {
				ranked.add(word);
			}
		}
		ranked.sort(Comparator.comparing((String w) -> frequencies.get(w)).reversed().thenComparing(Comparator.naturalOrder()));
		List<String> kept = ranked.subList(0, Math.min(MAX_WORDS, ranked.size()));
		try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(Path.of(args[1])));
				Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
			for (String word : kept) {
				writer.write(word);
				writer.write('\n');
			}
		}
		System.err.printf("%d dictionary words seen at least %d times, %d written%n", ranked.size(), MIN_COUNT, kept.size());
		return;
	}
}
