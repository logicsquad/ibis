package net.logicsquad.ibis;

import java.util.ArrayList;
import java.util.List;

public class SpellChecker {
	private final Dictionary dictionary;

	public SpellChecker(Dictionary dictionary) {
		this.dictionary = dictionary;
		return;
	}

	public List<Word> checkSpelling(Tokenizer tokenizer) {
		List<Word> result = new ArrayList<>();
		while (tokenizer.hasNext()) {
			Word w = tokenizer.next();
			if (!containsDigit(w) && !dictionary.isCorrect(w)) {
				result.add(w.withSuggestions(dictionary.suggestionsFor(w)));
			}
		}
		return result;
	}

	static boolean containsDigit(Word word) {
		for (int i = 0; i < word.length(); i++) {
			if (Character.isDigit(word.charAt(i))) {
				return true;
			}
		}
		return false;
	}
}
