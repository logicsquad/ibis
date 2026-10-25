package net.logicsquad.ibis;

import java.util.List;
import java.util.Locale;

/**
 * A {@code Handler} handles <em>special cases</em> for a {@link Tokenizer} by potentially breaking a {@link Word} into smaller component
 * {@link Word}s. The mechanism used by a {@link Tokenizer} may return some tokens in a form that are not suitable for a subsequent
 * spell-check. For example, hyphenated words may be returned with hyphens intact, whereas a dictionary is more likely to contain those
 * words in parts.
 * 
 * @author paulh
 * @since 0.2
 */
interface Handler {
	/**
	 * <p>
	 * Examines {@code word} for special cases, and returns a list that <em>either:</em>
	 * </p>
	 * 
	 * <ul>
	 * <li>contains {@code word} without alteration;</li>
	 * <li>contains a new {@link Word} with altered text content; or</li>
	 * <li>contains the component parts of {@code word}.</li>
	 * </ul>
	 * 
	 * @param word a {@link Word}
	 * @param text containing text for {@code word}
	 * @return list of one or more {@link Word}s
	 */
	List<Word> handle(Word word, String text);

	/**
	 * Returns a new {@link Handler} for {@link Locale} {@code locale}.
	 *
	 * @param locale a {@link Locale}
	 * @return new object
	 * @throws NullPointerException     if {@code locale} is {@code null}
	 * @throws IllegalArgumentException if the language of {@code locale} is not supported
	 */
	static Handler newInstance(Locale locale) {
		return switch (locale.getLanguage()) {
		case "en" -> new CaseHandler(List.of(new DashesCase(), new AbbreviationsCase(), new PossessiveCase()));
		default -> throw new IllegalArgumentException("Unsupported Locale: " + locale);
		}; 
	}
}
