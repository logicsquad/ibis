package net.logicsquad.ibis;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.language.Metaphone;

/**
 * A language that Ibis supports. Everything language-specific (phonetic encoder, built-in word lists, word frequencies and tokenizer
 * special cases) is selected here, by {@link Locale#getLanguage()}.
 *
 * @author paulh
 * @since 0.4
 */
enum Language {
	/**
	 * English
	 */
	ENGLISH(Locale.ENGLISH, Metaphone::new, List.of("/en/words-1.txt.gz", "/en/words-2.txt", "/en/names-1.txt.gz", "/en/names-2.txt", "/en/acronyms.txt"),
			"/en/frequency.txt.gz", () -> List.of(new DashesCase(), new AbbreviationsCase(), new PossessiveCase()));

	/**
	 * {@link Locale} for this language alone
	 */
	private final Locale locale;

	/**
	 * Supplier of {@link StringEncoder}s providing phonetic representations
	 */
	private final Supplier<StringEncoder> encoder;

	/**
	 * Names of built-in word list resources
	 */
	private final List<String> builtInWordLists;

	/**
	 * Name of the resource listing words in lower case, most common first
	 */
	private final String frequencyList;

	/**
	 * Supplier of {@link Case}s for a {@link Handler}
	 */
	private final Supplier<List<Case>> cases;

	/**
	 * Constructor
	 *
	 * @param locale           {@link Locale} for this language alone
	 * @param encoder          supplier of {@link StringEncoder}s
	 * @param builtInWordLists names of built-in word list resources
	 * @param frequencyList    name of the resource listing words in lower case, most common first
	 * @param cases            supplier of {@link Case}s
	 */
	Language(Locale locale, Supplier<StringEncoder> encoder, List<String> builtInWordLists, String frequencyList, Supplier<List<Case>> cases) {
		this.locale = locale;
		this.encoder = encoder;
		this.builtInWordLists = builtInWordLists;
		this.frequencyList = frequencyList;
		this.cases = cases;
		return;
	}

	/**
	 * Returns the {@code Language} of {@code locale}.
	 *
	 * @param locale a {@link Locale}
	 * @return {@code Language} of {@code locale}
	 * @throws NullPointerException     if {@code locale} is {@code null}
	 * @throws IllegalArgumentException if the language of {@code locale} is not supported
	 */
	static Language of(Locale locale) {
		Objects.requireNonNull(locale);
		for (Language language : values()) {
			if (language.locale.getLanguage().equals(locale.getLanguage())) {
				return language;
			}
		}
		throw new IllegalArgumentException("Unsupported Locale: " + locale);
	}

	/**
	 * Returns the {@link Locale} for this language alone, such as {@link Locale#ENGLISH}.
	 * 
	 * @return {@link Locale}
	 */
	Locale locale() {
		return locale;
	}

	/**
	 * Returns a new {@link StringEncoder} providing phonetic representations.
	 *
	 * @return new {@link StringEncoder}
	 */
	StringEncoder encoder() {
		return encoder.get();
	}

	/**
	 * Returns names of built-in word list resources.
	 *
	 * @return names of built-in word list resources
	 */
	List<String> builtInWordLists() {
		return builtInWordLists;
	}

	/**
	 * Returns the name of the resource listing words in lower case, most common first, one per line, compressed with GZip.
	 *
	 * @return name of frequency list resource
	 */
	String frequencyList() {
		return frequencyList;
	}

	/**
	 * Returns a new {@link Handler} for tokenizer special cases.
	 *
	 * @return new {@link Handler}
	 */
	Handler handler() {
		return new CaseHandler(cases.get());
	}
}
