package net.logicsquad.ibis;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

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
 * <p>
 * A {@code DefaultTokenizer} is immutable and thread-safe.
 * </p>
 * 
 * @author paulh
 * @since 0.1
 */
class DefaultTokenizer implements Tokenizer {
	/**
	 * Character replacements
	 */
	private static final Map<Character, Character> REPLACEMENTS = Map.of('\u2018', '\'', '\u2019', '\'', '\u055A', '\'', '\uFF07', '\'', '\u07F4', '\'',
			'\u02BC', '\'');

	/**
	 * {@link Locale} for {@link BreakIterator}s
	 */
	private final Locale locale;

	/**
	 * {@link Handler} for special cases
	 */
	private final Handler handler;

	/**
	 * {@link Rejector} to indicate {@link Word}s to omit
	 */
	private final Rejector rejector = new Rejector();

	/**
	 * Constructor
	 * 
	 * @param locale {@link Locale} for this tokenizer
	 * @throws NullPointerException     if {@code locale} is {@code null}
	 * @throws IllegalArgumentException if the language of {@code locale} is not supported
	 */
	DefaultTokenizer(Locale locale) {
		this.locale = Objects.requireNonNull(locale);
		handler = Handler.newInstance(locale);
		return;
	}

	/**
	 * Makes character replacements in {@code text} as defined by the replacements map, and returns the modified string. Each replacement is a
	 * single character, so positions in the result match those in {@code text}.
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

	@Override
	public Stream<Word> tokenize(String text) {
		String cleanText = cleanupText(text);
		// We need to call shouldReject() again, because handler.handle() may have changed the Word
		return candidates(cleanText).stream()
				.filter(w -> !rejector.shouldReject(w))
				.flatMap(w -> handler.handle(w, cleanText).stream())
				.filter(w -> !rejector.shouldReject(w));
	}

	/**
	 * Returns the {@link Word}s found in {@code text} by a {@link BreakIterator}. These are "candidates" because they might be rejected, or
	 * changed by the {@link Handler}.
	 * 
	 * @param text some text
	 * @return candidate {@link Word}s
	 */
	private List<Word> candidates(String text) {
		BreakIterator breakIterator = BreakIterator.getWordInstance(locale);
		breakIterator.setText(text);
		List<Word> result = new ArrayList<>();
		for (int start = breakIterator.first(), end = breakIterator.next(); end != BreakIterator.DONE; start = end, end = breakIterator.next()) {
			result.add(new Word(text.substring(start, end), start));
		}
		return result;
	}
}
