package net.logicsquad.ibis;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Assesses {@link Word}s for <em>rejection</em> by a {@link Tokenizer}. That is, indicates which words should be omitted from the
 * {@link Tokenizer}'s output, because they don't need to be checked in a {@link Dictionary}. This class rejects a {@link Word} where:
 * </p>
 * 
 * <ul>
 * <li>it doesn't start with a letter;</li>
 * <li>it contains one or more digits;</li>
 * <li>it is an internet protocol name (e.g., "http"); or</li>
 * <li>it appears to be a domain name.</li>
 * </ul>
 * 
 * @author paulh
 * @since 1.0
 */
public class Rejector {
	/**
	 * List of protocol names to reject
	 */
	private static final List<String> PROTOCOLS = List.of("http", "https", "ftp");

	/**
	 * Regex matching (most) domain names
	 */
	private static final String DOMAIN_REGEX = "^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+$";

	/**
	 * Should {@code word} be rejected?
	 * 
	 * @param word {@link Word} to test
	 * @return {@code true} if {@code word} should be rejected, otherwise {@code false}
	 * @throws NullPointerException if {@code word} is {@code null}
	 */
	public boolean reject(Word word) {
		Objects.requireNonNull(word);
		return word.length() < 2 || !Character.isLetter(word.text().charAt(0)) || containsDigit(word) || PROTOCOLS.contains(word.text()) || word.text().matches(DOMAIN_REGEX);
	}

	/**
	 * Does {@code word} contain at least one digit?
	 * 
	 * @param word {@link Word} to test
	 * @return {@code true} if {@code word} contains at least one digit, otherwise {@code false}
	 * @throws NullPointerException if {@code word} is {@code null}
	 */
	static boolean containsDigit(Word word) {
		Objects.requireNonNull(word);
		for (int i = 0; i < word.length(); i++) {
			if (Character.isDigit(word.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Does {@code word} appear to be a domain name?
	 * 
	 * @param word {@link Word} to test
	 * @return {@code true} if {@code word} appears to be a domain name, otherwise {@code false}
	 * @throws NullPointerException if {@code word} is {@code null}
	 */
	static boolean isDomainName(Word word) {
		Objects.requireNonNull(word);
		return word.text().matches(DOMAIN_REGEX);
	}
}
