package net.logicsquad.ibis.cli;

import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * <p>
 * Finds the text to check in an XML document: the text of its elements, but not their attributes, the document's comments or processing
 * instructions, or the text of elements to skip.
 * </p>
 *
 * <p>
 * The text of an element that has text of its own, such as a paragraph, is one {@link Passage}, along with the text of the elements
 * within it, such as an emphasised word, so that a word split by markup ({@code mis<emphasis>spel</emphasis>led}) is checked whole. Each
 * element within an element that has no text of its own, such as each cell in a table row, starts a passage of its own, so that their
 * text isn't run together. Two elements side by side, with no text between them, are more often alternatives, such as text shown under
 * different conditions, than parts of one word, so their text is separated, as is the text on either side of an empty element, which
 * usually stands for something, such as a cross-reference.
 * </p>
 *
 * <p>
 * A parser reads the document, and a {@link Cursor} follows it through the document's text, so that each character of a passage records
 * where it came from, whatever entity references, CDATA sections and line endings come before it.
 * </p>
 *
 * @author paulh
 * @since 0.7
 */
final class XmlText {
	/**
	 * Replacement text of the entities that XML predefines
	 */
	private static final Map<String, String> PREDEFINED = Map.of("lt", "<", "gt", ">", "amp", "&", "apos", "'", "quot", "\"");

	/**
	 * Start of a CDATA section
	 */
	private static final String CDATA_START = "<![CDATA[";

	/**
	 * End of a CDATA section
	 */
	private static final String CDATA_END = "]]>";

	/**
	 * Private constructor for static-only class
	 */
	private XmlText() {
		return;
	}

	/**
	 * Returns the passages to check in {@code xml}, in the order their elements start.
	 *
	 * @param xml  an XML document's text
	 * @param skip local names of elements whose text to leave out
	 * @return passages, none of them blank
	 * @throws XMLStreamException    if {@code xml} isn't well-formed
	 * @throws IllegalStateException if a {@link Cursor} can't follow the parser through {@code xml}
	 * @throws NullPointerException  if any argument is {@code null}
	 */
	static List<Passage> passages(String xml, Set<String> skip) throws XMLStreamException {
		Objects.requireNonNull(xml);
		Objects.requireNonNull(skip);
		Element root = parse(xml, skip);
		List<Builder> builders = new ArrayList<>();
		Builder passage = new Builder();
		builders.add(passage);
		collect(root, passage, builders);
		return builders.stream().filter(b -> !b.isBlank()).map(Builder::toPassage).toList();
	}

	/**
	 * Parses {@code xml} into a tree of its elements and their text, leaving out the elements to skip.
	 *
	 * @param xml  an XML document's text
	 * @param skip local names of elements whose text to leave out
	 * @return the root element
	 * @throws XMLStreamException if {@code xml} isn't well-formed
	 */
	private static Element parse(String xml, Set<String> skip) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		factory.setProperty(XMLInputFactory.IS_COALESCING, false);
		XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));
		Cursor cursor = new Cursor(xml);
		Deque<Element> open = new ArrayDeque<>();
		Element root = null;
		try {
			while (reader.hasNext()) {
				switch (reader.next()) {
				case XMLStreamConstants.START_ELEMENT -> {
					cursor.skipStartTag();
					Element parent = open.peek();
					Element element = new Element(parent != null && parent.skipped || skip.contains(reader.getLocalName()));
					if (parent == null) {
						root = element;
					} else if (!parent.skipped) {
						parent.children.add(element);
					}
					open.push(element);
				}
				case XMLStreamConstants.END_ELEMENT -> {
					cursor.skipEndTag();
					open.pop();
				}
				case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA, XMLStreamConstants.SPACE -> {
					String text = reader.getText();
					int[] offsets = cursor.follow(text);
					Element element = open.peek();
					if (element != null && !element.skipped) {
						element.children.add(new Characters(text, offsets));
					}
				}
				case XMLStreamConstants.COMMENT -> cursor.skip("<!--", "-->");
				case XMLStreamConstants.PROCESSING_INSTRUCTION -> cursor.skip("<?", "?>");
				case XMLStreamConstants.DTD -> cursor.skipDoctype();
				default -> {
					// Nothing else takes up any of the document's text
				}
				}
			}
		} finally {
			reader.close();
		}
		return root;
	}

	/**
	 * Adds the text of {@code element} to {@code passage}, and to {@code passages} a passage for each element within it that starts one.
	 *
	 * @param element  an element
	 * @param passage  the passage that {@code element}'s own text belongs to
	 * @param passages every passage so far, in the order their elements start
	 */
	private static void collect(Element element, Builder passage, List<Builder> passages) {
		boolean mixed = element.hasOwnText();
		Node previous = null;
		for (Node child : element.children) {
			switch (child) {
			case Characters characters -> passage.append(characters);
			case Element inner when inner.skipped || inner.children.isEmpty() -> passage.separate();
			case Element inner when mixed -> {
				if (previous instanceof Element) {
					passage.separate();
				}
				collect(inner, passage, passages);
			}
			case Element inner -> {
				passage.separate();
				Builder own = new Builder();
				passages.add(own);
				collect(inner, own, passages);
			}
			}
			previous = child;
		}
		return;
	}

	/**
	 * Some text or an element, within an element
	 */
	private sealed interface Node permits Characters, Element {
	}

	/**
	 * Some of an element's text
	 *
	 * @param text    the text, as the parser decoded it
	 * @param offsets offset of each character of {@code text} in the document's text
	 */
	private record Characters(String text, int[] offsets) implements Node {
	}

	/**
	 * An element, with its text and the elements within it, in order
	 */
	private static final class Element implements Node {
		/**
		 * Whether to leave out this element's text
		 */
		private final boolean skipped;

		/**
		 * This element's text and the elements within it, in order, or nothing if it's {@link #skipped}
		 */
		private final List<Node> children = new ArrayList<>();

		/**
		 * Constructor
		 *
		 * @param skipped whether to leave out this element's text
		 */
		private Element(boolean skipped) {
			this.skipped = skipped;
			return;
		}

		/**
		 * Does this element have text of its own, other than whitespace?
		 *
		 * @return {@code true} if this element has text of its own, otherwise {@code false}
		 */
		private boolean hasOwnText() {
			return children.stream().anyMatch(n -> n instanceof Characters characters && !characters.text().isBlank());
		}
	}

	/**
	 * Builds a {@link Passage}
	 */
	private static final class Builder {
		/**
		 * The passage's text
		 */
		private final StringBuilder text = new StringBuilder();

		/**
		 * Offset of each character of {@link #text}, and room for more
		 */
		private int[] offsets = new int[64];

		/**
		 * Adds {@code characters} to this passage.
		 *
		 * @param characters some of an element's text
		 */
		private void append(Characters characters) {
			int length = characters.text().length();
			ensureRoom(length);
			System.arraycopy(characters.offsets(), 0, offsets, text.length(), length);
			text.append(characters.text());
			return;
		}

		/**
		 * Ends the word that this passage ends with, if it ends with one.
		 */
		private void separate() {
			if (!text.isEmpty() && !Character.isWhitespace(text.charAt(text.length() - 1))) {
				ensureRoom(1);
				offsets[text.length()] = -1;
				text.append(' ');
			}
			return;
		}

		/**
		 * Makes room in {@link #offsets} for {@code more} characters.
		 *
		 * @param more number of characters
		 */
		private void ensureRoom(int more) {
			if (text.length() + more > offsets.length) {
				offsets = Arrays.copyOf(offsets, Math.max(offsets.length * 2, text.length() + more));
			}
			return;
		}

		/**
		 * Is this passage empty, or only whitespace?
		 *
		 * @return {@code true} if this passage is blank, otherwise {@code false}
		 */
		private boolean isBlank() {
			return text.toString().isBlank();
		}

		/**
		 * Returns this passage.
		 *
		 * @return passage
		 */
		private Passage toPassage() {
			return new Passage(text.toString(), Arrays.copyOf(offsets, text.length()));
		}
	}

	/**
	 * Follows a parser through a document's text, to find where each character of the text the parser reports comes from. The parser
	 * does the parsing, so this only needs to move past each piece of markup that the parser reports, and to match each character of text
	 * with the characters it came from, which differ for an entity reference, the start and end of a CDATA section, and a carriage return.
	 * If the parser and this ever disagree, this throws {@link IllegalStateException}, rather than give a wrong position.
	 */
	private static final class Cursor {
		/**
		 * The document's text
		 */
		private final String xml;

		/**
		 * Offset of the next character to move past
		 */
		private int position;

		/**
		 * Whether {@link #position} is in a CDATA section
		 */
		private boolean inCdata;

		/**
		 * Whether the last start tag was an empty-element tag, whose element the parser ends without an end tag
		 */
		private boolean emptyElement;

		/**
		 * Constructor
		 *
		 * @param xml the document's text
		 */
		private Cursor(String xml) {
			this.xml = xml;
			// The parser doesn't report the XML declaration
			if (xml.startsWith("<?xml") && xml.length() > 5 && isSpace(xml.charAt(5))) {
				position = xml.indexOf("?>") + 2;
			}
			return;
		}

		/**
		 * Moves past a start tag.
		 */
		private void skipStartTag() {
			toMarkup("<");
			char quote = 0;
			for (int i = position + 1; i < xml.length(); i++) {
				char c = xml.charAt(i);
				if (quote != 0) {
					quote = c == quote ? 0 : quote;
				} else if (c == '"' || c == '\'') {
					quote = c;
				} else if (c == '>') {
					emptyElement = xml.charAt(i - 1) == '/';
					position = i + 1;
					return;
				}
			}
			throw lost();
		}

		/**
		 * Moves past an end tag, which an empty-element tag doesn't have.
		 */
		private void skipEndTag() {
			if (emptyElement) {
				emptyElement = false;
				return;
			}
			skip("</", ">");
			return;
		}

		/**
		 * Moves past markup that starts with {@code start}, up to and including the next {@code end}.
		 *
		 * @param start start of the markup
		 * @param end   end of the markup
		 */
		private void skip(String start, String end) {
			toMarkup(start);
			int index = xml.indexOf(end, position + start.length());
			if (index < 0) {
				throw lost();
			}
			position = index + end.length();
			return;
		}

		/**
		 * Moves past a document type declaration, whose internal subset can contain {@code >} in brackets and quotes.
		 */
		private void skipDoctype() {
			toMarkup("<!DOCTYPE");
			char quote = 0;
			int depth = 0;
			for (int i = position; i < xml.length(); i++) {
				char c = xml.charAt(i);
				if (quote != 0) {
					quote = c == quote ? 0 : quote;
				} else if (c == '"' || c == '\'') {
					quote = c;
				} else if (c == '[') {
					depth++;
				} else if (c == ']') {
					depth--;
				} else if (c == '>' && depth == 0) {
					position = i + 1;
					return;
				}
			}
			throw lost();
		}

		/**
		 * Moves to markup that starts with {@code start}, past what the parser doesn't report: whitespace outside the root element, the end
		 * of a CDATA section, and an empty CDATA section.
		 *
		 * @param start start of the markup
		 */
		private void toMarkup(String start) {
			while (true) {
				if (inCdata && xml.startsWith(CDATA_END, position)) {
					position += CDATA_END.length();
					inCdata = false;
				} else if (!inCdata && xml.startsWith(CDATA_START + CDATA_END, position)) {
					position += CDATA_START.length() + CDATA_END.length();
				} else if (!inCdata && position < xml.length() && isSpace(xml.charAt(position))) {
					position++;
				} else {
					break;
				}
			}
			if (inCdata || !xml.startsWith(start, position)) {
				throw lost();
			}
			return;
		}

		/**
		 * Moves past the characters that the parser reported as {@code text}, and returns the offset of each.
		 *
		 * @param text text, as the parser decoded it
		 * @return offset of each character of {@code text} in the document's text
		 */
		private int[] follow(String text) {
			int[] offsets = new int[text.length()];
			int i = 0;
			while (i < text.length()) {
				if (position >= xml.length()) {
					throw lost();
				}
				char c = xml.charAt(position);
				if (inCdata && xml.startsWith(CDATA_END, position)) {
					position += CDATA_END.length();
					inCdata = false;
				} else if (!inCdata && xml.startsWith(CDATA_START, position)) {
					position += CDATA_START.length();
					inCdata = true;
				} else if (!inCdata && c == '&') {
					int end = xml.indexOf(';', position);
					String replacement = end < 0 ? null : replacement(xml.substring(position + 1, end));
					if (replacement == null || !text.startsWith(replacement, i)) {
						throw lost();
					}
					for (int k = 0; k < replacement.length(); k++) {
						offsets[i++] = position;
					}
					position = end + 1;
				} else if (c == '\r') {
					// The parser reports a carriage return, and one followed by a line feed, as a line feed
					if (text.charAt(i) != '\n') {
						throw lost();
					}
					offsets[i++] = position;
					position += xml.startsWith("\r\n", position) ? 2 : 1;
				} else {
					if (text.charAt(i) != c) {
						throw lost();
					}
					offsets[i++] = position++;
				}
			}
			return offsets;
		}

		/**
		 * Returns the replacement text of the entity reference {@code &name;}.
		 *
		 * @param name the reference's name, or {@code #} and a decimal number, or {@code #x} and a hexadecimal one
		 * @return replacement text, or {@code null} if it's not a character reference or a predefined entity's
		 */
		private static String replacement(String name) {
			try {
				if (name.startsWith("#x")) {
					return Character.toString(Integer.parseInt(name.substring(2), 16));
				} else if (name.startsWith("#")) {
					return Character.toString(Integer.parseInt(name.substring(1)));
				}
			} catch (IllegalArgumentException e) {
				return null;
			}
			return PREDEFINED.get(name);
		}

		/**
		 * Is {@code c} whitespace, as XML defines it?
		 *
		 * @param c a character
		 * @return {@code true} if {@code c} is whitespace, otherwise {@code false}
		 */
		private static boolean isSpace(char c) {
			return c == ' ' || c == '\t' || c == '\r' || c == '\n';
		}

		/**
		 * Returns an exception for when this and the parser disagree.
		 *
		 * @return exception
		 */
		private IllegalStateException lost() {
			return new IllegalStateException("Lost track of the parser at offset " + position + ".");
		}
	}
}
