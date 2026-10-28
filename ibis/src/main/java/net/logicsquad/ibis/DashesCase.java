package net.logicsquad.ibis;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
	public List<Word> handle(Word word, String text) {
		Objects.requireNonNull(word);
		Objects.requireNonNull(text);
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
			parts.add(new Word(sb.toString(), start + word.start()));
			while (i < word.length() && isDash(word.text().charAt(i))) {
				i++;
			}
		}
		if (parts.isEmpty()) {
			int from = Math.clamp(word.start() - 50L, 0, text.length());
			int to = Math.clamp(word.end() + 50L, from, text.length());
			LOG.error("Unable to handle {} in context:'{}'", word, text.substring(from, to));
			throw new IllegalArgumentException("No text found.");
		}
		return parts;
	}

	static boolean isDash(char c) {
		return c == '\u002D' || c == '\u2013' || c == '\u2014';
	}

	@Override
	public boolean recurseOnResult() {
		return true;
	}
}
