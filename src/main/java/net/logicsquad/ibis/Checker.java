package net.logicsquad.ibis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A simple spelling checker. A {@code Checker} requires a {@link Dictionary} on construction, and a word {@link Tokenizer} over the text to
 * check when calling {@link #checkSpelling(Tokenizer)}. It returns each word that the {@link Dictionary} doesn't recognise as a
 * {@link Misspelling}. A {@code Checker} is immutable and thread-safe.
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
	 * Constructor that will create {@code Locale.ENGLISH} dictionary
	 * 
	 * @since 0.2
	 */
	public Checker() {
		this.dictionary = Dictionary.builder(Locale.ENGLISH).addWords().build();
		return;
	}

	/**
	 * Constructor
	 * 
	 * @param dictionary a {@link Dictionary}
	 * @throws NullPointerException if {@code dictionary} is {@code null}
	 */
	public Checker(Dictionary dictionary) {
		Objects.requireNonNull(dictionary);
		this.dictionary = dictionary;
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
		return checkSpelling(Tokenizer.newInstance(Locale.ENGLISH, text));
	}

	/**
	 * Checks spelling of text represented by {@link Tokenizer}, returning a {@link List} of {@link Misspelling}s.
	 * 
	 * @param tokenizer a {@link Tokenizer}
	 * @return list of {@link Misspelling}s
	 * @throws NullPointerException if {@code tokenizer} is {@code null}
	 */
	public List<Misspelling> checkSpelling(Tokenizer tokenizer) {
		Objects.requireNonNull(tokenizer);
		List<Misspelling> result = new ArrayList<>();
		while (tokenizer.hasNext()) {
			Word w = tokenizer.next();
			if (!dictionary.isCorrect(w)) {
				result.add(new Misspelling(w, dictionary.suggestionsFor(w)));
			}
		}
		return result;
	}
}
