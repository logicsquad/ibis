package net.logicsquad.ibis;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a word in the context of a larger body of text. {@code Word}s are generally created by a tokenizer, and represent the
 * individual words to check in a dictionary. If a {@code Word} is unknown to a dictionary, it can optionally carry a list of suggestions
 * (as {@code String}s). {@code Word}s are immutable and thread-safe.
 * 
 * @author paulh
 * @since 1.0
 */
public class Word {
	/**
	 * Text content
	 */
	private final String text;

	/**
	 * Start position in containing text
	 */
	private final int start;

	/**
	 * Optional list of suggestions
	 */
	private final List<String> suggestions;

	/**
	 * Constructor
	 * 
	 * @param text  text content
	 * @param start start position in containing text
	 * @throws NullPointerException     if {@code text} is {@code null}
	 * @throws IllegalArgumentException if {@code start} is negative
	 */
	private Word(String text, int start) {
		this(text, start, null);
	}

	/**
	 * Constructor
	 * 
	 * @param text        text content
	 * @param start       start position in containing text
	 * @param suggestions optional list of suggestions
	 * @throws NullPointerException     if {@code text} is {@code null}
	 * @throws IllegalArgumentException if {@code start} is negative
	 */
	private Word(String text, int start, List<String> suggestions) {
		Objects.requireNonNull(text);
		if (start < 0) {
			throw new IllegalArgumentException("start cannot be negative.");
		}
		this.text = text;
		this.start = start;
		this.suggestions = suggestions;
		return;
	}

	/**
	 * Returns text content.
	 * 
	 * @return text content
	 */
	public String text() {
		return text;
	}

	/**
	 * Returns start position in containing text.
	 * 
	 * @return start position in containing text
	 */
	public int start() {
		return start;
	}

	/**
	 * Returns index <em>after</em> final character of this {@code Word} in containing text.
	 * 
	 * @return index <em>after</em> final character
	 */
	public int end() {
		return start + text.length();
	}

	/**
	 * Returns a new {@code Word}.
	 * 
	 * @param text  text content
	 * @param start start position in containing text
	 * @return new object
	 * @throws NullPointerException     if {@code text} is {@code null}
	 * @throws IllegalArgumentException if {@code start} is negative
	 */
	public static Word of(String text, int start) {
		return of(text, start, null);
	}

	/**
	 * Returns a new {@code Word}.
	 * 
	 * @param text        text content
	 * @param start       start position in containing text
	 * @param suggestions optional list of suggestions
	 * @return new object
	 * @throws NullPointerException     if {@code text} is {@code null}
	 * @throws IllegalArgumentException if {@code start} is negative
	 */
	public static Word of(String text, int start, List<String> suggestions) {
		return new Word(text, start, suggestions);
	}

	@Override
	public int hashCode() {
		return Objects.hash(start, text);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Word other)) {
			return false;
		}
		return start == other.start && Objects.equals(text, other.text);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Word [start=");
		sb.append(start);
		sb.append(", text=");
		sb.append(text);
		if (suggestions != null) {
			sb.append(", suggestions=(");
			sb.append(String.join(", ", suggestions));
			sb.append(")");
		}
		sb.append(']');
		return sb.toString();
	}

	/**
	 * Returns length of text content.
	 * 
	 * @return length of text content
	 */
	public int length() {
		return text().length();
	}

	/**
	 * Returns {@code char} at position {@code index}.
	 * 
	 * @param index index into text content
	 * @return {@code char} at position {@code index}
	 * @throws IndexOutOfBoundsException if {@code index} is negative or not less than length of text content
	 */
	public char charAt(int index) {
		return text().charAt(index);
	}

	/**
	 * Returns list of suggestions provided by a dictionary. The result may be {@code null} or empty.
	 * 
	 * @return list of suggestions
	 */
	public List<String> suggestions() {
		return Collections.unmodifiableList(suggestions);
	}

	/**
	 * Returns a copy of this {@code Word} with suggestion list set to {@code suggestions}.
	 * 
	 * @param suggestions a list of suggestions for the new {@code Word}
	 * @return new object
	 */
	public Word withSuggestions(List<String> suggestions) {
		return new Word(text, start, suggestions);
	}

	/**
	 * Returns {@link #text} as lower case.
	 * 
	 * @return {@link #text} as lower case
	 */
	String toLowerCase() {
		return text.toLowerCase();
	}

	/**
	 * Returns {@link #text} with first character upper case, and remainder lower case.
	 * 
	 * @return {@link #text} with initial cap
	 */
	String toInitialCap() {
		return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
	}
}
