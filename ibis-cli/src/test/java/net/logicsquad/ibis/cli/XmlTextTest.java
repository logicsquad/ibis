package net.logicsquad.ibis.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests on {@link XmlText}.
 *
 * @author paulh
 */
public class XmlTextTest {
	@Test
	public void checksTextButNotAttributesCommentsOrInstructions() throws XMLStreamException {
		assertEquals(List.of("One  two three"), texts("<p title=\"Teh\">One <!-- teh --> two<?pi teh?> three</p>"));
		return;
	}

	@Test
	public void joinsWordSplitByInlineMarkup() throws XMLStreamException {
		assertEquals(List.of("misspelled words"), texts("<para>mis<emphasis>spel</emphasis>led words</para>"));
		return;
	}

	@Test
	public void startsPassageForEachElementInElementWithoutText() throws XMLStreamException {
		assertEquals(List.of("Customer", "type"), texts("<row><entry>Customer</entry><entry>type</entry></row>"));
		assertEquals(List.of("See  here", "Note"), texts("<para>See <footnote><para>Note</para></footnote> here</para>"));
		return;
	}

	// Only one of these phrases appears, but text runs into the phrase beside it
	@Test
	public void separatesElementsSideBySide() throws XMLStreamException {
		assertEquals(List.of("The director directors must"),
				texts("<para>The <phrase c=\"one\">director</phrase><phrase c=\"many\">directors</phrase> must</para>"));
		assertEquals(List.of("Officerand"), texts("<para>Officer<phrase c=\"more\">and</phrase></para>"));
		return;
	}

	@Test
	public void separatesWordsAtEmptyElement() throws XMLStreamException {
		assertEquals(List.of("one two"), texts("<p>one<xref linkend=\"x\"/>two</p>"));
		return;
	}

	@Test
	public void leavesOutElementsToSkip() throws XMLStreamException {
		String xml = "<article><info><title>Teh <emphasis>title</emphasis></title></info><para>Text</para></article>";
		assertEquals(List.of("Text"), texts(xml, "info"));
		assertEquals(List.of("Teh title", "Text"), texts(xml));
		return;
	}

	@Test
	public void recordsOffsetsDespiteEntitiesCdataAndLineEnds() throws XMLStreamException {
		String xml = "<p>AT&amp;T\r\nteh <![CDATA[x<y]]> &#x1F600;ok</p>";
		Passage passage = XmlText.passages(xml, Set.of()).get(0);
		assertEquals("AT&T\nteh x<y 😀ok", passage.text());
		assertEquals(xml.indexOf("&amp;"), passage.offset(2));
		assertEquals(xml.indexOf("T\r"), passage.offset(3));
		assertEquals(xml.indexOf("\r\n"), passage.offset(4));
		assertOffset(xml, passage, "teh");
		assertOffset(xml, passage, "x<y");
		assertEquals(xml.indexOf("&#x1F600;"), passage.offset(passage.text().indexOf('\uDE00')));
		assertOffset(xml, passage, "ok");
		return;
	}

	@Test
	public void recordsOffsetsAfterDeclarationInstructionsAndDoctype() throws XMLStreamException {
		String xml = """
				<?xml version="1.0" encoding="UTF-8"?>
				<?dbfo orientation="landscape"?>
				<!DOCTYPE a [ <!ELEMENT a (#PCDATA)> ]>
				<a title="a>b">word</a>
				""";
		assertOffset(xml, XmlText.passages(xml, Set.of()).get(0), "word");
		return;
	}

	@Test
	public void throwsForMalformedXml() {
		assertThrows(XMLStreamException.class, () -> XmlText.passages("<p>unclosed", Set.of()));
		assertThrows(XMLStreamException.class, () -> XmlText.passages("<p>&nbsp;</p>", Set.of()));
		return;
	}

	/**
	 * Returns the text of each passage in {@code xml}.
	 *
	 * @param xml  an XML document
	 * @param skip local names of elements to skip
	 * @return text of each passage
	 * @throws XMLStreamException if {@code xml} isn't well-formed
	 */
	private static List<String> texts(String xml, String... skip) throws XMLStreamException {
		return XmlText.passages(xml, Set.of(skip)).stream().map(Passage::text).toList();
	}

	/**
	 * Asserts that each character of {@code text} in {@code passage} came from the same character of {@code text} in {@code xml}.
	 *
	 * @param xml     an XML document
	 * @param passage a passage in {@code xml}
	 * @param text    text that appears once in each
	 */
	private static void assertOffset(String xml, Passage passage, String text) {
		int index = passage.text().indexOf(text);
		for (int i = 0; i < text.length(); i++) {
			assertEquals(xml.indexOf(text) + i, passage.offset(index + i));
		}
		return;
	}
}
