package net.logicsquad.ibis;

import java.util.List;
import java.util.function.Predicate;

/**
 * Represents a <em>case</em> for handling by a {@link CaseHandler}.
 *
 * @author paulh
 * @since 0.2
 */
interface Case {
	/**
	 * Returns a {@link Predicate} answering: is {@code word} suitable for handling by this {@code Case}? That is, would this {@code Case}
	 * modify {@code word}?
	 *
	 * @return a {@link Predicate} returning {@code true} if this {@code Case} would handle {@code word}, otherwise {@code false}
	 */
	Predicate<Word> predicate();

	/**
	 * Handles {@code word}. This method shares its contract with {@link Handler#handle(Word, String)}.
	 * 
	 * @param word a {@link Word}
	 * @param text containing text for {@code word}
	 * @return list of one or more {@link Word}s
	 * @see Handler#handle(Word, String)
	 */
	List<Word> handle(Word word, String text);

	/**
	 * Might the {@link Word} returned by {@link #handle(Word, String)} require further modification? For example, a {@code Case} that
	 * splits a {@link Word} into component parts might need {@link Handler#handle(Word, String)} called again on each of those parts, and should
	 * return {@code true}. If a case is "atomic" (that is, it makes a modification that will require no further examination), this method
	 * should return {@code false}.
	 * 
	 * @return {@code true} if {@link Handler#handle(Word, String)} should be called again on any results, otherwise {@code false}
	 */
	boolean recurseOnResult();
}
