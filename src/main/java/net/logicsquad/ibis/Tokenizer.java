package net.logicsquad.ibis;

import java.text.BreakIterator;

public class Tokenizer {
	private final BreakIterator breakIterator = BreakIterator.getWordInstance();

	private final String text;

	private int start;

	private int end;

	public Tokenizer(String text) {
		this.text = text;
		breakIterator.setText(text);
		start = breakIterator.first();
		end = breakIterator.next();
		return;
	}

	public boolean hasNext() {
		return end != BreakIterator.DONE;
	}

	private String candidateNext() {
		String word = text.substring(start, end);
		start = end;
		end = breakIterator.next();
		return word;
	}

	public String next() {
		String word = candidateNext();
		while (!Character.isLetterOrDigit(word.charAt(0))) {
			word = candidateNext();
		}
		return word;
	}
}
