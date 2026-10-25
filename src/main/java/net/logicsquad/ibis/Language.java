package net.logicsquad.ibis;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.commons.codec.StringEncoder;
import org.apache.commons.codec.language.Metaphone;

/**
 * A language that Ibis supports. Everything language-specific (phonetic encoder, built-in word lists and tokenizer special cases) is selected
 * here, by {@link Locale#getLanguage()}.
 *
 * @author paulh
 * @since 0.4
 */
enum Language {
	/**
	 * English
	 */
	ENGLISH("en", Metaphone::new, List.of("/en/words-1.txt.gz", "/en/words-2.txt", "/en/names-1.txt.gz", "/en/names-2.txt", "/en/acronyms.txt"),
			() -> List.of(new DashesCase(), new AbbreviationsCase(), new PossessiveCase()));

	/**
	 * ISO 639 language code
	 */
	private final String code;

	/**
	 * Supplier of {@link StringEncoder}s providing phonetic representations
	 */
	private final Supplier<StringEncoder> encoder;

	/**
	 * Names of built-in word list resources
	 */
	private final List<String> builtInWordLists;

	/**
	 * Supplier of {@link Case}s for a {@link Handler}
	 */
	private final Supplier<List<Case>> cases;

	/**
	 * Constructor
	 *
	 * @param code             ISO 639 language code
	 * @param encoder          supplier of {@link StringEncoder}s
	 * @param builtInWordLists names of built-in word list resources
	 * @param cases            supplier of {@link Case}s
	 */
	Language(String code, Supplier<StringEncoder> encoder, List<String> builtInWordLists, Supplier<List<Case>> cases) {
		this.code = code;
		this.encoder = encoder;
		this.builtInWordLists = builtInWordLists;
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
			if (language.code.equals(locale.getLanguage())) {
				return language;
			}
		}
		throw new IllegalArgumentException("Unsupported Locale: " + locale);
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
	 * Returns a new {@link Handler} for tokenizer special cases.
	 *
	 * @return new {@link Handler}
	 */
	Handler handler() {
		return new CaseHandler(cases.get());
	}
}
