package net.logicsquad.ibis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A simple spelling checker. A {@code Checker} requires a {@link Dictionary} on construction, and a word {@link Tokenizer} over the text to
 * check when calling {@link #checkSpelling(Tokenizer)}.
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
	 * Checks spelling of {@code text}, returning a {@link List} of incorrect {@link Word}s, along with suggestions for the correct word.
	 * 
	 * @param text text to check
	 * @return list of incorrect {@link Word}s with suggestions
	 * @throws NullPointerException if {@code text} is {@code null}
	 * @since 0.2
	 */
	public List<Word> checkSpelling(String text) {
		Objects.requireNonNull(text);
		return checkSpelling(Tokenizer.newInstance(Locale.ENGLISH, text));
	}

	/**
	 * Checks spelling of text represented by {@link Tokenizer}, returning a {@link List} of incorrect {@link Word}s, along with suggestions for the correct
	 * word.
	 * 
	 * @param tokenizer a {@link Tokenizer}
	 * @return list of incorrect {@link Word}s with suggestions
	 * @throws NullPointerException if {@code tokenizer} is {@code null}
	 */
	public List<Word> checkSpelling(Tokenizer tokenizer) {
		Objects.requireNonNull(tokenizer);
		List<Word> result = new ArrayList<>();
		while (tokenizer.hasNext()) {
			Word w = tokenizer.next();
			if (!dictionary.isCorrect(w)) {
				result.add(w.withSuggestions(dictionary.suggestionsFor(w)));
			}
		}
		return result;
	}
}
