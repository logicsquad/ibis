package net.logicsquad.ibis;

import java.util.Locale;
import java.util.Objects;

/**
 * Represents a word in the context of a larger body of text. {@code Word}s are created by a {@link Tokenizer}, and represent the individual
 * words to check in a {@link Dictionary}. {@code Word}s are immutable and thread-safe.
 *
 * @param text  text content
 * @param start start position in containing text
 * @author paulh
 * @since 0.1
 */
public record Word(String text, int start) {
	/**
	 * Constructor
	 *
	 * @param text  text content
	 * @param start start position in containing text
	 * @throws NullPointerException     if {@code text} is {@code null}
	 * @throws IllegalArgumentException if {@code start} is negative
	 */
	public Word {
		Objects.requireNonNull(text);
		if (start < 0) {
			throw new IllegalArgumentException("start cannot be negative.");
		}
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
	 * Returns length of text content.
	 *
	 * @return length of text content
	 */
	int length() {
		return text.length();
	}

	/**
	 * Returns {@code char} at position {@code index}.
	 *
	 * @param index index into text content
	 * @return {@code char} at position {@code index}
	 * @throws IndexOutOfBoundsException if {@code index} is negative or not less than length of text content
	 */
	char charAt(int index) {
		return text.charAt(index);
	}

	/**
	 * Returns {@link #text} as lower case, using the rules of {@link Locale#ROOT} rather than the default {@link Locale}.
	 *
	 * @return {@link #text} as lower case
	 */
	String toLowerCase() {
		return text.toLowerCase(Locale.ROOT);
	}

	/**
	 * Returns {@link #text} with first character upper case, and remainder lower case, using the rules of {@link Locale#ROOT} rather than
	 * the default {@link Locale}. If {@link #text} is empty, returns it unchanged.
	 *
	 * @return {@link #text} with initial cap
	 */
	String toInitialCap() {
		return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1).toLowerCase(Locale.ROOT);
	}
}
