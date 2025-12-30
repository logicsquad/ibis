package net.logicsquad.ibis;

import java.util.Locale;

/**
 * Tokenizes text into {@link Word}s.
 * 
 * @author paulh
 * @since 0.2
 */
public interface Tokenizer {
	/**
	 * Returns the raw text supplied to the constructor.
	 * 
	 * @return raw text
	 */
	String rawText();

	/**
	 * Returns the <em>potentially modified</em> text used in tokenization.
	 * 
	 * @return <em>potentially modified</em> text
	 */
	String text();

	/**
	 * Are there more {@link Word}s to return?
	 * 
	 * @return {@code true} if there are more {@link Word}s, otherwise {@code false}
	 */
	boolean hasNext();

	/**
	 * Returns the next {@link Word}.
	 * 
	 * @return next {@link Word}
	 */
	Word next();

	/**
	 * Returns a {@code Tokenizer} for {@code text}.
	 *
	 * @param text a string
	 * @return {@link Tokenizer}
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	static Tokenizer newInstance(Locale locale, String text) {
		return new DefaultTokenizer(locale, text);
	}
}
