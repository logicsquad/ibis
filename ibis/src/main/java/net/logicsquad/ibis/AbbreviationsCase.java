package net.logicsquad.ibis;

import java.util.List;
import java.util.function.Predicate;

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
	/**
	 * {@link Predicate} for this {@code Case}
	 */
	private static final Predicate<Word> PREDICATE = word -> "e.g".equalsIgnoreCase(word.text()) || "i.e".equalsIgnoreCase(word.text());

	@Override
	public Predicate<Word> predicate() {
		return PREDICATE;
	}

	@Override
	public List<Word> handle(Word word, String text) {
		if (word.end() < text.length() && '.' == text.charAt(word.end())) {
			return List.of(new Word(word.text() + ".", word.start()));
		} else {
			return List.of(word);
		}
	}

	@Override
	public boolean recurseOnResult() {
		return false;
	}
}
