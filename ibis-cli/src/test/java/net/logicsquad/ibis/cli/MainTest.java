package net.logicsquad.ibis.cli;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the command line on files in a temporary folder.
 *
 * @author paulh
 */
public class MainTest {
	@TempDir
	Path temp;

	private final ByteArrayOutputStream out = new ByteArrayOutputStream();

	private final ByteArrayOutputStream err = new ByteArrayOutputStream();

	@Test
	public void reportsMisspellingWithLineColumnAndSuggestions() throws IOException {
		Path file = write("party.txt", "Now is the\ntime for teh party.\n");
		assertEquals(Main.MISSPELLED, run(file.toString()));
		assertTrue(out().startsWith(file + ":2:10: teh (the, "), out());
		assertEquals(1, out().lines().count());
		assertEquals("", err());
		return;
	}

	@Test
	public void reportsNothingForCorrectText() throws IOException {
		assertEquals(Main.OK, run(write("fine.txt", "Now is the time.\n").toString()));
		assertEquals("", out());
		return;
	}

	@Test
	public void checksTextOfXml() throws IOException {
		Path file = write("doc.xml", """
				<?xml version="1.0"?>
				<article title="Tehh">
				  <para>A\tteh <emphasis>mis</emphasis>spelled word</para>
				</article>
				""");
		assertEquals(Main.MISSPELLED, run(file.toString()));
		assertTrue(out().startsWith(file + ":3:17: teh ("), out());
		assertEquals(1, out().lines().count());
		return;
	}

	@Test
	public void skipsElementsAndAcceptsWordLists() throws IOException {
		Path file = write("doc.xml", "<article><info><author>Zarquon</author></info><para>Zarquon wrote it.</para></article>");
		Path words = write("words.txt", "Zarquon\n");
		assertEquals(Main.MISSPELLED, run("--skip", "info", file.toString()));
		assertEquals(1, out().lines().count());
		out.reset();
		assertEquals(Main.OK, run("--words", words.toString(), file.toString()));
		assertEquals("", out());
		return;
	}

	@Test
	public void reportsMalformedXmlAndChecksOtherFiles() throws IOException {
		Path bad = write("bad.xml", "<p>unclosed teh");
		Path text = write("party.txt", "teh party");
		assertEquals(Main.FAILED, run(bad.toString(), text.toString()));
		assertTrue(err().startsWith(bad + ":1:"), err());
		assertTrue(err().contains(" error: "), err());
		assertTrue(out().startsWith(text + ":1:1: teh ("), out());
		return;
	}

	@Test
	public void readsXmlInItsDeclaredEncoding() throws IOException {
		Path file = temp.resolve("latin.xml");
		Files.write(file, "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<p title=\"é\">teh</p>".getBytes(StandardCharsets.ISO_8859_1));
		assertEquals(Main.MISSPELLED, run(file.toString()));
		assertTrue(out().startsWith(file + ":2:14: teh ("), out());
		return;
	}

	@Test
	public void reportsFilesAndWordListsThatCantBeRead() throws IOException {
		Path latin = temp.resolve("latin.txt");
		Files.write(latin, "café".getBytes(StandardCharsets.ISO_8859_1));
		assertEquals(Main.FAILED, run(latin.toString()));
		assertTrue(err().contains("cannot read " + latin + ": it isn't valid UTF-8."), err());
		err.reset();
		Path missing = temp.resolve("missing.txt");
		assertEquals(Main.FAILED, run(missing.toString()));
		assertTrue(err().contains("cannot read " + missing + ": there's no such file."), err());
		err.reset();
		assertEquals(Main.FAILED, run("--words", missing.toString(), write("fine.txt", "Fine.").toString()));
		assertTrue(err().contains("cannot read the word list " + missing + ": there's no such file."), err());
		return;
	}

	@Test
	public void rejectsBadArguments() {
		assertEquals(Main.FAILED, run());
		assertTrue(err().contains("Give a file to check."), err());
		assertTrue(err().contains("Usage: ibis"), err());
		err.reset();
		assertEquals(Main.FAILED, run("--frob", "file.txt"));
		assertTrue(err().contains("Unknown option --frob."), err());
		err.reset();
		assertEquals(Main.FAILED, run("file.txt", "--words"));
		assertTrue(err().contains("--words needs a value."), err());
		return;
	}

	@Test
	public void doubleDashEndsOptions() {
		assertEquals(Main.FAILED, run("--", "-x"));
		assertTrue(err().contains("cannot read -x"), err());
		assertFalse(err().contains("Unknown option"), err());
		return;
	}

	@Test
	public void showsHelpAndVersion() {
		assertEquals(Main.OK, run("--help"));
		assertTrue(out().startsWith("Usage: ibis"), out());
		out.reset();
		assertEquals(Main.OK, run("--version"));
		assertEquals("ibis (development)", out().strip());
		return;
	}

	/**
	 * Runs the command line with {@code args}.
	 *
	 * @param args command-line arguments
	 * @return exit status
	 */
	private int run(String... args) {
		return Main.run(args, new PrintStream(out, true, UTF_8), new PrintStream(err, true, UTF_8));
	}

	/**
	 * Writes {@code text} to a file named {@code name} in the temporary folder.
	 *
	 * @param name a file name
	 * @param text the file's text
	 * @return the file
	 * @throws IOException if the file can't be written
	 */
	private Path write(String name, String text) throws IOException {
		return Files.writeString(temp.resolve(name), text);
	}

	private String out() {
		return out.toString(UTF_8);
	}

	private String err() {
		return err.toString(UTF_8);
	}
}
