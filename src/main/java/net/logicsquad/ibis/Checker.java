package net.logicsquad.ibis;

import java.util.ArrayList;
import java.util.List;

/**
 * Spelling checker.
 * 
 * @author paulh
 * @since 1.0
 */
public class Checker {
	/**
	 * Dictionary providing correct spellings
	 */
	private final Dictionary dictionary;

	/**
	 * Constructor
	 * 
	 * @param dictionary a {@link Dictionary}
	 */
	public Checker(Dictionary dictionary) {
		this.dictionary = dictionary;
		return;
	}

	public List<Word> checkSpelling(Tokenizer tokenizer) {
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
