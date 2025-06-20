package net.logicsquad.ibis;

import java.util.Queue;
import java.util.function.Predicate;

/**
 * Strips a terminal {@code 's} character sequence (denoting a possessive form in English) from a {@link Word}.
 * 
 * @author paulh
 * @since 0.2
 */
class PossessiveCase implements Case {
	/**
	 * {@link Predicate} for this {@code Case}
	 */
	private static final Predicate<Word> PREDICATE = word -> word.text().endsWith("'s");

	@Override
	public Predicate<Word> predicate() {
		return PREDICATE;
	}

	@Override
	public Word handle(Word word, String text, Queue<Word> queue) {
		return Word.of(word.text().substring(0, word.length() - 2), word.start());
	}

	@Override
	public boolean recurseOnResult() {
		return false;
	}
}
