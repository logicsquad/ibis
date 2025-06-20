package net.logicsquad.ibis;

import java.util.Queue;

/**
 * Represents a <em>case</em> for handling by a {@link CaseHandler}.
 *
 * @author paulh
 * @since 0.2
 */
interface Case {
	/**
	 * Is {@code word} suitable for handling by this {@code Case}? That is, would this {@code Case} modify {@code word}?
	 *
	 * @param word a {@link Word}
	 * @return {@code true} if this {@code Case} would handle {@code word}, otherwise {@code false}
	 */
	boolean predicate(Word word); // FIXME: why not return Predicate?

	/**
	 * Handles {@code word}. This method shares its contract with {@link Handler#handle(Word, String, Queue)}.
	 * 
	 * @param word  a {@link Word}
	 * @param text  containing text for {@code word}
	 * @param queue a {@link Queue}
	 * @return potentially modified {@link Word}
	 * @see Handler#handle(Word, String, Queue)
	 */
	Word handle(Word word, String text, Queue<Word> queue);

	/**
	 * Might the {@link Word} returned by {@link #handle(Word, String, Queue)} require further modification? For example, a {@code Case} that
	 * splits a {@link Word} into component parts might need {@link Handler#handle(Word, String, Queue)} called again on those parts, and should
	 * return {@code true}. If a case is "atomic" (that is, it makes a modification that will require no further examination), this method
	 * should return {@code false}.
	 * 
	 * @return {@code true} if {@link Handler#handle(Word, String, Queue)} should be called again on any results, otherwise {@code false}
	 */
	boolean recurseOnResult();
}
