package net.logicsquad.ibis;

import java.text.BreakIterator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Tokenizes text into {@link Word}s. While this class uses a {@link BreakIterator}, it also:
 * </p>
 * 
 * <ul>
 * <li>replaces problematic in-word characters;</li>
 * <li>drops {@link Word}s rejected by its {@link Rejector}; and</li>
 * <li>potentially splits words returned by the {@link BreakIterator} into more than one {@link Word} using a {@link Handler}.</li>
 * </ul>
 * 
 * @author paulh
 * @since 1.0
 */
public class Tokenizer {
	/**
	 * Character replacements
	 */
	private static final Map<Character, Character> REPLACEMENTS = Map.of('\u2018', '\'', '\u2019', '\'', '\u055A', '\'', '\uFF07', '\'', '\u07F4', '\'',
			'\u02BC', '\'');

	/**
	 * A {@link BreakIterator} to perform initial tokenization
	 */
	private final BreakIterator breakIterator = BreakIterator.getWordInstance();

	/**
	 * Text to tokenize
	 */
	private final String text;

	/**
	 * Holds original text if {@link #text} was modified
	 */
	private final String rawText;

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
	private final Deque<Word> queue = new LinkedList<>();

	/**
	 * {@link Handler} for special cases
	 */
	private final Handler handler = new Handler();

	/**
	 * {@link Rejector} to indicate {@link Word}s to omit
	 */
	private final Rejector rejector = new Rejector();

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
		if (containsReplacement(text)) {
			this.text = cleanupText(text);
			this.rawText = text;
		} else {
			this.text = text;
			this.rawText = null;
		}
		breakIterator.setText(this.text);
		start = breakIterator.first();
		end = breakIterator.next();
		primeNext();
		return;
	}

	/**
	 * Does {@code text} contain any characters in the replacements list?
	 * 
	 * @param text some text
	 * @return {@code true} if {@code text} contains any characters in the replacements list, otherwise {@code false}
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	private static boolean containsReplacement(String text) {
		Objects.requireNonNull(text);
		for (Character c : REPLACEMENTS.keySet()) {
			if (text.indexOf(c) != -1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Makes character replacements in {@code text} as defined by the replacements map, and returns the modified string.
	 * 
	 * @param text some text
	 * @return {@code text} with character replacements made
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	private static String cleanupText(String text) {
		Objects.requireNonNull(text);
		String result = text;
		for (var entry : REPLACEMENTS.entrySet()) {
			result = result.replace(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * Returns the raw text supplied to the constructor.
	 * 
	 * @return raw text
	 */
	public String rawText() {
		return rawText == null ? text : rawText;
	}

	/**
	 * Returns the <em>potentially modified</em> text used in tokenization.
	 * 
	 * @return <em>potentially modified</em> text
	 */
	public String text() {
		return text;
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
		if (!queue.isEmpty()) {
			next = queue.removeFirst();
			return;
		}
		Word candidate = candidateNext();
		Word word = candidate == null ? null : handler.handle(candidate, text, queue);
		// We need to call shouldReject() again below, because handler.handle() may have changed the Word
		while (word != null && rejector.shouldReject(word)) {
			if (queue.isEmpty()) {
                do {
                    candidate = candidateNext();
                } while (candidate != null);
				word = candidate == null ? null : handler.handle(candidate, text, queue);
			} else {
				word = queue.removeFirst();
			}
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
		while (end != BreakIterator.DONE) {
			Word word = Word.of(text.substring(start, end), start);
			start = end;
			end = breakIterator.next();
			if (!rejector.shouldReject(word)) {
				return word;
			}
		}
		return null;
	}

	/**
	 * Returns the next {@link Word}.
	 * 
	 * @return next {@link Word}
	 */
	public Word next() {
		Word word = next;
		primeNext();
		return word;
	}
}
