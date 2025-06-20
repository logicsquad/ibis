package net.logicsquad.ibis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Splits a {@link Word} on any of:
 * </p>
 * 
 * <ol>
 * <li>hyphen;</li>
 * <li>en-dash; or</li>
 * <li>em-dash</li>
 * </ol>
 * 
 * <p>
 * ensuring that each component is then returned by the {@link Tokenizer}.
 * </p>
 * 
 * @author paulh
 * @since 0.2
 */
class DashesCase implements Case {
	/**
	 * Logger
	 */
	private static final Logger LOG = LoggerFactory.getLogger(DashesCase.class);
	
	/**
	 * {@link Predicate} for this {@code Case}
	 */
	private static final Predicate<Word> PREDICATE = word -> word.text().contains("-") || word.text().contains("–") || word.text().contains("—");

	@Override
	public Predicate<Word> predicate() {
		return PREDICATE;
	}

	@Override
	public Word handle(Word word, String text, Queue<Word> queue) {
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
			LOG.error("Unable to handle {} in context:'{}'", word, text.substring(word.start() - 50, word.end() + 50));
			throw new IllegalArgumentException("No text found.");
		} else if (parts.size() > 1) {
			// skip(1) because we want to return the first element below
			parts.stream().skip(1).forEach(p -> queue.add(handle(p, text, queue)));
		}
		return parts.getFirst();
	}

	static boolean isDash(char c) {
		return c == '\u002D' || c == '\u2013' || c == '\u2014';
	}

	@Override
	public boolean recurseOnResult() {
		return true;
	}
}
