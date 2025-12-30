package net.logicsquad.ibis;

import java.util.List;
import java.util.Locale;
import java.util.Queue;

/**
 * A {@code Handler} handles <em>special cases</em> for a {@link Tokenizer} by potentially breaking a {@link Word} into smaller component
 * {@link Word}s. The mechanism used by a {@link Tokenizer} may return some tokens in a form that are not suitable for a subsequent
 * spell-check. For example, hyphenated words may be returned with hyphens intact, whereas a dictionary is more likely to contain those
 * words in parts.
 * 
 * @author paulh
 * @since 0.2
 */
public interface Handler {
	/**
	 * <p>
	 * Examines {@code word} for special cases, and <em>either:</em>
	 * </p>
	 * 
	 * <ul>
	 * <li>returns {@code word} without alteration;</li>
	 * <li>alters the text content of {@code word}, returning a new {@link Word}; or</li>
	 * <li>breaks {@code word} into component parts, returning the first {@link Word} and pushing any remaining parts onto {@code queue}.</li>
	 * </ul>
	 *
	 * @param word  a {@link Word}
	 * @param text  containing text for {@code word}
	 * @param queue a {@link Queue}
	 * @return a {@link Word} as described above
	 */
	Word handle(Word word, String text, Queue<Word> queue);

	/**
	 * Returns a new {@link Handler} for {@link Locale} {@code locale}.
	 *
	 * @return new object
	 */
	static Handler newInstance(Locale locale) {
		return switch (locale.getLanguage()) {
		case "en" -> new CaseHandler(List.of(new DashesCase(), new AbbreviationsCase(), new PossessiveCase()));
		default -> throw new IllegalArgumentException("Unsupported Locale: " + locale);
		}; 
	}
}
