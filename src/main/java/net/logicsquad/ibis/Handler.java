package net.logicsquad.ibis;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Handles special cases for a {@link Tokenizer} by potentially breaking a {@link Word} into smaller component {@link Word}s. The
 * {@link java.text.BreakIterator BreakIterator} used by a {@link Tokenizer} will return some tokens in a form that are not suitable for a
 * subsequent spell-check. For example, hyphenated words are returned with hyphens intact, whereas a dictionary is more likely to contain
 * those words in parts. This handler handles the following cases:
 * </p>
 * 
 * <ul>
 * <li>strips <em>all</em> dash characters and breaks the {@link Word} into component {@link Word}s;</li>
 * <li>reverts "e.g" and "i.e" (where the terminal period has been removed) back to "e.g." and "i.e." (as long as there was a terminal
 * period in the original text); and</li>
 * <li>removes a terminal "'s" denoting a possessive form.</li>
 * </ul>
 * 
 * @author paulh
 * @since 1.0
 */
public class Handler {
	/**
	 * Logger
	 */
	private static final Logger LOG = LoggerFactory.getLogger(Handler.class);

	/**
	 * Handles {@code word} (in the larger context of {@code text}). If the {@link Word} needs to be modified as a special case, a new
	 * {@link Word} is returned for the initial part, and subsequent parts are added as {@link Word} to the {@code queue}. Otherwise,
	 * {@code word} is simply returned.
	 * 
	 * @param word  a {@link Word}
	 * @param text  text containing {@link Word}
	 * @param queue {@link Tokenizer}'s queue
	 * @return a {@link Word} after handling special cases
	 * @throws NullPointerException if any argument is {@code null}
	 */
	public Word handle(Word word, String text, Deque<Word> queue) {
		Objects.requireNonNull(word);
		Objects.requireNonNull(text);
		Objects.requireNonNull(queue);
		if (word.text().contains("-") || word.text().contains("–") || word.text().contains("—")) {
			return handle(handleDashes(word, text, queue), text, queue);
		} else if ("e.g".equals(word.toLowerCase()) || "i.e".equals(word.toLowerCase())) {
			if ('.' == text.charAt(word.end())) {
				return Word.of(word.text() + ".", word.start());
			} else {
				return word;
			}
		} else if (word.text().endsWith("'s")) {
			return Word.of(word.text().substring(0, word.length() - 2), word.start());
		} else {
			return word;
		}
	}

	/**
	 * Handles any dash characters in {@code word} by breaking it into component parts at those dashes. The first part is returned as a new
	 * {@link Word}, and subsequent parts are added to {@code queue}.
	 * 
	 * @param word  a {@link Word} containing dashes
	 * @param text  text containing {@link Word}
	 * @param queue {@link Tokenizer}'s queue
	 * @return first part as a new {@link Word}
	 * @throws NullPointerException if any argument is {@code null}
	 */
	private Word handleDashes(Word word, String text, Deque<Word> queue) {
		Objects.requireNonNull(word);
		Objects.requireNonNull(text);
		Objects.requireNonNull(queue);
		List<Word> parts = new ArrayList<>();
		int i = 0;
		while (i < word.length() && isDash(word.text().charAt(i))) {
			i++;
		}
		while (i < word.length()) {
			int start = i;
			StringBuilder sb = new StringBuilder();
			while (i < word.length() && !isDash(word.text().charAt(i))) {
				sb.append(word.text().charAt(i));
				i++;
			}
			parts.add(Word.of(sb.toString(), start + word.start()));
			while (i < word.length() && isDash(word.text().charAt(i))) {
				i++;
			}
		}
		if (parts.isEmpty()) {
			LOG.error("Unable to handle {}.", word);
			throw new IllegalArgumentException("No text found.");
		} else if (parts.size() > 1) {
			for (int j = parts.size() - 1; j > 0; j--) {
				queue.addFirst(handle(parts.get(j), text, queue));
			}
		}
		return parts.getFirst();
	}

	/**
	 * <p>
	 * Is {@code c} a <em>dash</em> character? More specifically, is {@code c} any of:
	 * </p>
	 * 
	 * <ul>
	 * <li>hyphen (U+002D);</li>
	 * <li>en-dash (U+2013); or</li>
	 * <li>em-dash (U+2014).</li>
	 * </ul>
	 * 
	 * @param c a {@code char}
	 * @return {@code true} if {@code c} is a dash character, otherwise {@code false}
	 */
	static boolean isDash(char c) {
		return c == '\u002D' || c == '\u2013' || c == '\u2014';
	}
}
