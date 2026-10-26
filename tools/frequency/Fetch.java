import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads the frequency corpus into a directory, as plain text files: the Project Gutenberg books listed in
 * {@code tools/frequency/gutenberg.txt}, from the Gutenberg mirror at gutenberg.pglaf.org, and the Compilation of Presidential Documents
 * for the years in {@link #PRESIDENTIAL_YEARS}, from govinfo.gov. Run with:
 *
 * <pre>
 * java tools/frequency/Fetch.java &lt;corpus directory&gt;
 * </pre>
 *
 * It waits a second between requests, and skips files already downloaded, so it can be stopped and run again.
 */
public class Fetch {
	private static final String GUTENBERG = "https://gutenberg.pglaf.org/cache/epub/%1$d/pg%1$d.txt";
	private static final String PRESIDENTIAL = "https://www.govinfo.gov/content/pkg/DCPD-%1$d%2$05d/html/DCPD-%1$d%2$05d.htm";
	private static final List<Integer> PRESIDENTIAL_YEARS = List.of(2019, 2024);

	// Documents are numbered from 1 each year, so this many missing in a row means the year is done
	private static final int MAX_MISSING = 25;

	private static final Pattern GUTENBERG_TEXT = Pattern.compile("(?s)\\*\\*\\* ?START OF TH(?:E|IS) PROJECT GUTENBERG EBOOK[^\n]*\n(.*)\\*\\*\\* ?END OF TH(?:E|IS) PROJECT GUTENBERG EBOOK");
	private static final Pattern ENTITY = Pattern.compile("&(#x?[0-9a-fA-F]+|amp|lt|gt|quot|apos|nbsp);");

	private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

	public static void main(String[] args) throws Exception {
		Path corpus = Path.of(args[0]);
		Path gutenberg = Files.createDirectories(corpus.resolve("gutenberg"));
		for (String line : Files.readAllLines(Path.of("tools/frequency/gutenberg.txt"))) {
			int number = Integer.parseInt(line.strip());
			Path file = gutenberg.resolve(number + ".txt");
			if (Files.exists(file)) {
				continue;
			}
			String body = get(GUTENBERG.formatted(number));
			Matcher matcher = body == null ? null : GUTENBERG_TEXT.matcher(body);
			if (matcher == null || !matcher.find()) {
				System.err.println("Skipping Gutenberg " + number + ": not found, or no start and end markers.");
				continue;
			}
			Files.writeString(file, matcher.group(1));
		}
		Path presidential = Files.createDirectories(corpus.resolve("presidential"));
		for (int year : PRESIDENTIAL_YEARS) {
			int missing = 0;
			for (int number = 1; missing < MAX_MISSING; number++) {
				Path file = presidential.resolve("%d%05d.txt".formatted(year, number));
				if (Files.exists(file)) {
					missing = 0;
					continue;
				}
				String body = get(PRESIDENTIAL.formatted(year, number));
				if (body == null) {
					missing++;
					continue;
				}
				missing = 0;
				Files.writeString(file, presidentialText(body));
			}
		}
		return;
	}

	// Returns the document's text: no styles, tags or catalogue entries ("Categories:" and after), and entities decoded
	private static String presidentialText(String html) {
		String text = html.replaceAll("(?is)<style.*?</style>", " ").replaceAll("(?s)<[^>]*>", " ");
		text = ENTITY.matcher(text).replaceAll(m -> Matcher.quoteReplacement(decode(m.group(1))));
		int categories = text.indexOf("Categories:");
		return categories < 0 ? text : text.substring(0, categories);
	}

	private static String decode(String entity) {
		return switch (entity) {
		case "amp" -> "&";
		case "lt" -> "<";
		case "gt" -> ">";
		case "quot" -> "\"";
		case "apos" -> "'";
		case "nbsp" -> " ";
		default -> Character.toString(entity.startsWith("#x") ? Integer.parseInt(entity.substring(2), 16) : Integer.parseInt(entity.substring(1)));
		};
	}

	// Returns the body at url, or null if it isn't found, after waiting a second. govinfo.gov redirects missing documents to an error
	// page, so a redirect counts as not found.
	private static String get(String url) throws IOException, InterruptedException {
		Thread.sleep(1000);
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("User-Agent", "Ibis word frequency tool (https://github.com/logicsquad/ibis)")
				.timeout(Duration.ofSeconds(60))
				.build();
		HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() == 404 || response.statusCode() / 100 == 3) {
			return null;
		}
		if (response.statusCode() != 200) {
			throw new IOException("HTTP " + response.statusCode() + " for " + url);
		}
		return response.body();
	}
}
