package net.logicsquad.ibis;

import java.util.ArrayList;
import java.util.List;

public class Checker {
	private final Dictionary dictionary;

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
