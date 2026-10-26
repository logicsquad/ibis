package net.logicsquad.ibis;

import java.util.Locale;
import java.util.stream.Stream;

/**
 * Tokenizes text into {@link Word}s. A {@code Tokenizer} is a strategy that a {@link Checker} holds, so implementations must be thread-safe,
 * and can tokenize any number of texts.
 * 
 * @author paulh
 * @since 0.2
 */
public interface Tokenizer {
	/**
	 * Returns the {@link Word}s to check in {@code text}, in order.
	 * 
	 * @param text a string
	 * @return a {@link Stream} of {@link Word}s
	 * @throws NullPointerException if {@code text} is {@code null}
	 * @since 0.4
	 */
	Stream<Word> tokenize(String text);

	/**
	 * Returns the built-in {@code Tokenizer} for {@code locale}.
	 *
	 * @param locale a {@link Locale}
	 * @return {@link Tokenizer}
	 * @throws NullPointerException     if {@code locale} is {@code null}
	 * @throws IllegalArgumentException if the language of {@code locale} is not supported
	 * @since 0.4
	 */
	static Tokenizer forLocale(Locale locale) {
		return new DefaultTokenizer(locale);
	}
}
