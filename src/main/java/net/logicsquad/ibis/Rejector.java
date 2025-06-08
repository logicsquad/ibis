package net.logicsquad.ibis;

public class Rejector {
	public boolean reject(Word word) {
		return !Character.isLetterOrDigit(word.text().charAt(0)) || containsDigit(word);
	}

	static boolean containsDigit(Word word) {
		for (int i = 0; i < word.length(); i++) {
			if (Character.isDigit(word.charAt(i))) {
				return true;
			}
		}
		return false;
	}
}
