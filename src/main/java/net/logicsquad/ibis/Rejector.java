package net.logicsquad.ibis;

import java.util.List;

public class Rejector {
	private static final List<String> PROTOCOLS = List.of("http", "https", "ftp");

	private static final String DOMAIN_REGEX = "^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+$";

	public boolean reject(Word word) {
		return !Character.isLetter(word.text().charAt(0)) || containsDigit(word) || PROTOCOLS.contains(word.text()) || word.text().matches(DOMAIN_REGEX);
	}

	static boolean containsDigit(Word word) {
		for (int i = 0; i < word.length(); i++) {
			if (Character.isDigit(word.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	static boolean isDomainName(Word word) {
		return word.text().matches(DOMAIN_REGEX);
	}
}
