package net.logicsquad.ibis.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import net.logicsquad.ibis.Checker;
import net.logicsquad.ibis.Dictionary;
import net.logicsquad.ibis.Misspelling;

/**
 * <p>
 * Checks the spelling in text and XML files from the command line, and reports each misspelling in GNU's format for messages, as
 * {@code file:line:column: word (suggestions)}, so that an editor such as Emacs can go to it:
 * </p>
 *
 * <pre>
 * java -jar ibis-cli.jar [--words &lt;file&gt;]... [--skip &lt;element&gt;]... &lt;file&gt;...
 * </pre>
 *
 * <p>
 * A file whose name ends in {@code .xml} is checked as XML, as {@link XmlText} describes, and any other as UTF-8 text.
 * </p>
 *
 * @author paulh
 * @since 0.7
 * @see <a href="https://github.com/logicsquad/ibis/issues/29">#29</a>
 */
public final class Main {
	/**
	 * Exit status when no file has a misspelling
	 */
	static final int OK = 0;

	/**
	 * Exit status when a file has a misspelling
	 */
	static final int MISSPELLED = 1;

	/**
	 * Exit status for bad arguments, a word list or file that can't be read, or a file that isn't well-formed XML
	 */
	static final int FAILED = 2;

	/**
	 * Help
	 */
	private static final String USAGE = String.join(System.lineSeparator(),
			"Usage: ibis [options] <file>...",
			"Checks the spelling in each file: as XML if its name ends in .xml, and as",
			"UTF-8 text otherwise. Reports each misspelling as",
			"file:line:column: word (suggestions), and exits with status 1 if there",
			"are any, or 2 if a file can't be checked.",
			"",
			"  --words <file>    Accept the words in <file> too, one per line, gzipped",
			"                    or not. Give it once for each word list.",
			"  --skip <element>  Leave out the text of XML elements with this local",
			"                    name, such as DocBook's info. Give it once for each",
			"                    element.",
			"  --help            Show this help.",
			"  --version         Show Ibis's version.");

	/**
	 * Matches the encoding in an XML declaration
	 */
	private static final Pattern ENCODING = Pattern.compile("^<\\?xml[^>]*?\\sencoding\\s*=\\s*[\"']([A-Za-z][A-Za-z0-9._-]*)[\"']");

	/**
	 * Where to report misspellings
	 */
	private final PrintStream out;

	/**
	 * Where to report problems
	 */
	private final PrintStream err;

	/**
	 * Word lists to accept the words in
	 */
	private final List<Path> wordLists = new ArrayList<>();

	/**
	 * Local names of XML elements whose text to leave out
	 */
	private final Set<String> skip = new HashSet<>();

	/**
	 * Files to check, as given
	 */
	private final List<String> files = new ArrayList<>();

	/**
	 * Constructor
	 *
	 * @param out where to report misspellings
	 * @param err where to report problems
	 */
	private Main(PrintStream out, PrintStream err) {
		this.out = out;
		this.err = err;
		return;
	}

	/**
	 * Checks the files given, and exits with the status that {@link #run(String[], PrintStream, PrintStream)} returns.
	 *
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		System.exit(run(args, System.out, System.err));
		return;
	}

	/**
	 * Checks the files given.
	 *
	 * @param args command-line arguments
	 * @param out  where to report misspellings
	 * @param err  where to report problems
	 * @return exit status: {@link #OK}, {@link #MISSPELLED} or {@link #FAILED}
	 */
	static int run(String[] args, PrintStream out, PrintStream err) {
		Main main = new Main(out, err);
		Integer status = main.parse(args);
		return status == null ? main.check() : status;
	}

	/**
	 * Reads the arguments.
	 *
	 * @param args command-line arguments
	 * @return an exit status if there's nothing to check, otherwise {@code null}
	 */
	private Integer parse(String[] args) {
		boolean options = true;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (options && ("--help".equals(arg) || "-h".equals(arg))) {
				out.println(USAGE);
				return OK;
			} else if (options && "--version".equals(arg)) {
				String version = Main.class.getPackage().getImplementationVersion();
				out.println("ibis " + (version == null ? "(development)" : version));
				return OK;
			} else if (options && ("--words".equals(arg) || "--skip".equals(arg))) {
				if (i + 1 == args.length) {
					return usage(arg + " needs a value.");
				}
				String value = args[++i];
				if ("--words".equals(arg)) {
					wordLists.add(Path.of(value));
				} else {
					skip.add(value);
				}
			} else if (options && "--".equals(arg)) {
				options = false;
			} else if (options && arg.startsWith("-") && arg.length() > 1) {
				return usage("Unknown option " + arg + ".");
			} else {
				files.add(arg);
			}
		}
		if (files.isEmpty()) {
			return usage("Give a file to check.");
		}
		return null;
	}

	/**
	 * Checks each file.
	 *
	 * @return exit status
	 */
	private int check() {
		Dictionary.Builder builder = Dictionary.builder(Locale.ENGLISH).addBuiltInWords();
		for (Path wordList : wordLists) {
			try {
				builder.addWords(wordList);
			} catch (UncheckedIOException e) {
				return failed("cannot read the word list " + wordList + ": " + reason(e.getCause()) + ".");
			}
		}
		Checker checker = new Checker(wordLists.isEmpty() ? Dictionary.builtIn(Locale.ENGLISH) : builder.build());
		int status = OK;
		for (String file : files) {
			status = Math.max(status, check(file, checker));
		}
		return status;
	}

	/**
	 * Checks {@code file}, and reports its misspellings in the order they appear.
	 *
	 * @param file    a file, as given
	 * @param checker a {@link Checker}
	 * @return exit status
	 */
	private int check(String file, Checker checker) {
		boolean xml = file.toLowerCase(Locale.ROOT).endsWith(".xml");
		String text;
		List<Passage> passages;
		try {
			text = read(Path.of(file), xml);
			passages = xml ? XmlText.passages(text, skip) : List.of(new Passage(text, null));
		} catch (IOException e) {
			return failed("cannot read " + file + ": " + reason(e) + ".");
		} catch (XMLStreamException e) {
			Location location = e.getLocation();
			String where = location == null || location.getLineNumber() < 0 ? "" : location.getLineNumber() + ":" + location.getColumnNumber() + ":";
			err.println(file + ":" + where + " error: " + message(e));
			return FAILED;
		} catch (IllegalStateException e) {
			return failed("cannot check " + file + ": " + e.getMessage());
		}
		List<Found> found = new ArrayList<>();
		for (Passage passage : passages) {
			for (Misspelling misspelling : checker.checkSpelling(passage.text())) {
				found.add(new Found(passage.offset(misspelling.word().start()), misspelling));
			}
		}
		found.sort(Comparator.comparingInt(Found::offset));
		Lines lines = new Lines(text);
		for (Found f : found) {
			List<String> suggestions = f.misspelling().suggestions();
			out.println(file + ":" + lines.line(f.offset()) + ":" + lines.column(f.offset()) + ": " + f.misspelling().word().text()
					+ (suggestions.isEmpty() ? "" : " (" + String.join(", ", suggestions) + ")"));
		}
		return found.isEmpty() ? OK : MISSPELLED;
	}

	/**
	 * A misspelling, and its offset in a file's text
	 *
	 * @param offset      offset of the misspelled word in the file's text
	 * @param misspelling the misspelling
	 */
	private record Found(int offset, Misspelling misspelling) {
	}

	/**
	 * Returns the text of {@code path}, without a byte order mark: in UTF-8, or for XML, in the encoding its byte order mark or XML
	 * declaration gives.
	 *
	 * @param path a file
	 * @param xml  whether {@code path} is XML
	 * @return text of {@code path}
	 * @throws IOException if {@code path} can't be read, or isn't in its encoding
	 */
	private static String read(Path path, boolean xml) throws IOException {
		byte[] bytes = Files.readAllBytes(path);
		Charset charset = xml ? encoding(bytes) : StandardCharsets.UTF_8;
		String text;
		try {
			text = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(bytes)).toString();
		} catch (CharacterCodingException e) {
			throw new IOException("it isn't valid " + charset.name(), e);
		}
		return text.startsWith("\uFEFF") ? text.substring(1) : text;
	}

	/**
	 * Returns the encoding of an XML document: UTF-16 if it starts with a UTF-16 byte order mark, the encoding its XML declaration gives
	 * if it has one, and otherwise UTF-8.
	 *
	 * @param bytes an XML document
	 * @return its encoding
	 * @throws IOException if Java doesn't support the encoding
	 */
	private static Charset encoding(byte[] bytes) throws IOException {
		if (bytes.length >= 2 && (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF || bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE)) {
			return StandardCharsets.UTF_16;
		}
		Matcher matcher = ENCODING.matcher(new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.ISO_8859_1));
		if (!matcher.find()) {
			return StandardCharsets.UTF_8;
		}
		try {
			return Charset.forName(matcher.group(1));
		} catch (IllegalArgumentException e) {
			throw new IOException("its encoding, " + matcher.group(1) + ", isn't supported", e);
		}
	}

	/**
	 * Returns why {@code e} happened, in words.
	 *
	 * @param e an exception
	 * @return reason
	 */
	private static String reason(Throwable e) {
		if (e instanceof NoSuchFileException) {
			return "there's no such file";
		} else if (e instanceof AccessDeniedException) {
			return "permission denied";
		} else if (e instanceof CharacterCodingException) {
			return "it isn't valid UTF-8";
		}
		return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
	}

	/**
	 * Returns the message of {@code e}, without the location that the JDK's parser puts first.
	 *
	 * @param e an exception from a parser
	 * @return message
	 */
	private static String message(XMLStreamException e) {
		String message = String.valueOf(e.getMessage());
		int index = message.indexOf("Message: ");
		return index < 0 ? message : message.substring(index + "Message: ".length());
	}

	/**
	 * Reports {@code problem}.
	 *
	 * @param problem a problem
	 * @return {@link #FAILED}
	 */
	private int failed(String problem) {
		err.println("ibis: " + problem);
		return FAILED;
	}

	/**
	 * Reports {@code problem} with the arguments, and the help.
	 *
	 * @param problem a problem
	 * @return {@link #FAILED}
	 */
	private int usage(String problem) {
		err.println("ibis: " + problem);
		err.println(USAGE);
		return FAILED;
	}
}
