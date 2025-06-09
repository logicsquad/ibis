package net.logicsquad.ibis;

import java.text.BreakIterator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

/**
 * <p>
 * Tokenizes text into {@link Word}s. While this class uses a {@link BreakIterator}, it also:
 * </p>
 * 
 * <ul>
 * <li>drops {@link Word}s rejected by its {@link Rejector}; and</li>
 * <li>potentially splits words returned by the {@link BreakIterator} into more than one {@link Word} using a {@link Handler}.</li>
 * </ul>
 * 
 * @author paulh
 * @since 1.0
 */
public class Tokenizer {
	/**
	 * A {@link BreakIterator} to perform initial tokenization
	 */
	private final BreakIterator breakIterator = BreakIterator.getWordInstance();

	/**
	 * Text to tokenize
	 */
	private final String text;

	/**
	 * Cursor on start position of a word
	 */
	private int start;

	/**
	 * Cursor on end position of a word
	 */
	private int end;

	/**
	 * A queue to buffer additional {@link Word}s from the {@link Handler}
	 */
	private Deque<Word> queue = new LinkedList<>();

	/**
	 * {@link Handler} for special cases
	 */
	private Handler handler = new Handler();

	/**
	 * {@link Rejector} to indicate {@link Word}s to omit
	 */
	private Rejector rejector = new Rejector();

	/**
	 * Next {@link Word} to return
	 */
	private Word next = null;

	/**
	 * Constructor
	 * 
	 * @param text a string
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	public Tokenizer(String text) {
		Objects.requireNonNull(text);
		this.text = text;
		breakIterator.setText(text);
		start = breakIterator.first();
		end = breakIterator.next();
		primeNext();
		return;
	}

	/**
	 * Are there more {@link Word}s to return?
	 * 
	 * @return {@code true} if there are more {@link Word}s, otherwise {@code false}
	 */
	public boolean hasNext() {
		return next != null;
	}

	/**
	 * Sets {@link #next} to the next {@link Word}, or {@code null} if there are no more {@link Word}s
	 */
	private void primeNext() {
		Word word = candidateNext();
		while (word != null && rejector.reject(word)) {
			word = candidateNext();
		}
		next = word;
		return;
	}

	/**
	 * Returns the next {@link Word} from the {@link BreakIterator}. This is a "candidate" because it might be rejected (and it might be
	 * {@code null}).
	 * 
	 * @return candidate next {@link Word}
	 */
	private Word candidateNext() {
		if (end == BreakIterator.DONE) {
			return null;
		} else {
			Word word = Word.of(text.substring(start, end), start);
			start = end;
			end = breakIterator.next();
			return word;
		}
	}

	/**
	 * Returns the next {@link Word}.
	 * 
	 * @return next {@link Word}
	 */
	public Word next() {
		if (!queue.isEmpty()) {
			return queue.removeFirst();
		}
		Word word = next;
		primeNext();
		return handler.handle(word, text, queue);
	}
}
