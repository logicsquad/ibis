package net.logicsquad.ibis;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A simple spelling checker. A {@code Checker} holds a {@link Dictionary} and a {@link Tokenizer}, and {@link #checkSpelling(String)}
 * returns each word in a text that the {@link Dictionary} doesn't recognise as a {@link Misspelling}. A {@code Checker} is immutable and
 * thread-safe.
 *
 * @author paulh
 * @since 0.1
 */
public class Checker {
	/**
	 * Dictionary providing correct spellings
	 */
	private final Dictionary dictionary;

	/**
	 * Tokenizer finding words to check
	 */
	private final Tokenizer tokenizer;

	/**
	 * Constructor that will create {@code Locale.ENGLISH} dictionary
	 *
	 * @since 0.2
	 */
	public Checker() {
		this(Dictionary.builder(Locale.ENGLISH).addWords().build());
		return;
	}

	/**
	 * Constructor using the built-in {@link Tokenizer} for {@code Locale.ENGLISH}
	 *
	 * @param dictionary a {@link Dictionary}
	 * @throws NullPointerException if {@code dictionary} is {@code null}
	 */
	public Checker(Dictionary dictionary) {
		this(dictionary, Tokenizer.forLocale(Locale.ENGLISH));
		return;
	}

	/**
	 * Constructor
	 *
	 * @param dictionary a {@link Dictionary}
	 * @param tokenizer  a {@link Tokenizer}
	 * @throws NullPointerException if either argument is {@code null}
	 * @since 0.4
	 */
	public Checker(Dictionary dictionary, Tokenizer tokenizer) {
		this.dictionary = Objects.requireNonNull(dictionary);
		this.tokenizer = Objects.requireNonNull(tokenizer);
		return;
	}

	/**
	 * Checks spelling of {@code text}, returning a {@link List} of {@link Misspelling}s.
	 *
	 * @param text text to check
	 * @return list of {@link Misspelling}s
	 * @throws NullPointerException if {@code text} is {@code null}
	 * @since 0.2
	 */
	public List<Misspelling> checkSpelling(String text) {
		Objects.requireNonNull(text);
		return tokenizer.tokenize(text)
				.filter(w -> !dictionary.isCorrect(w.text()))
				.map(w -> new Misspelling(w, dictionary.suggestionsForIncorrect(w.text())))
				.toList();
	}
}
