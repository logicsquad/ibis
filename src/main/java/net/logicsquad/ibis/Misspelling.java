package net.logicsquad.ibis;

import java.util.List;
import java.util.Objects;

/**
 * Represents a {@link Word} that a {@link Checker} found to be misspelled, along with suggestions for its correct spelling.
 * {@code Misspelling}s are immutable and thread-safe.
 *
 * @param word        the misspelled {@link Word}
 * @param suggestions suggestions for the correct spelling, which may be empty but is never {@code null}
 * @author paulh
 * @since 0.4
 */
public record Misspelling(Word word, List<String> suggestions) {
	/**
	 * Constructor
	 *
	 * @param word        the misspelled {@link Word}
	 * @param suggestions suggestions for the correct spelling, which are copied
	 * @throws NullPointerException if either argument is {@code null}, or {@code suggestions} contains {@code null}
	 */
	public Misspelling {
		Objects.requireNonNull(word);
		suggestions = List.copyOf(suggestions);
	}
}
