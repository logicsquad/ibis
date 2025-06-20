package net.logicsquad.ibis;

import java.util.Queue;

/**
 * <p>
 * A tokenizer might strip a trailing period from certain abbreviations that appear with that
 * punctuation intact in a {@link Dictionary}. This class makes the following transformations:
 * </p>
 * 
 * <ol>
 * <li>{@literal e.g} → {@literal e.g.};</li>
 * <li>{@literal E.g} → {@literal E.g.};</li>
 * <li>{@literal i.e} → {@literal i.e.}; and</li>
 * <li>{@literal I.e} → {@literal I.e.}.</li>
 * </ol>
 */
class AbbreviationsCase implements Case {
	@Override
	public boolean predicate(Word word) {
		return "e.g".equals(word.toLowerCase()) || "i.e".equals(word.toLowerCase());
	}

	@Override
	public Word handle(Word word, String text, Queue<Word> queue) {
		if ('.' == text.charAt(word.end())) {
			return Word.of(word.text() + ".", word.start());
		} else {
			return word;
		}
	}

	@Override
	public boolean recurseOnResult() {
		return false;
	}
}
