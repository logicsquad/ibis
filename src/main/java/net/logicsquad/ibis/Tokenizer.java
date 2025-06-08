package net.logicsquad.ibis;

import java.text.BreakIterator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

public class Tokenizer {
	private final BreakIterator breakIterator = BreakIterator.getWordInstance();

	private final String text;

	private int start;

	private int end;

	private Deque<Word> queue = new LinkedList<>(); 

	private Handler handler = new Handler();

	private Word next = null;

	/**
	 * Constructor
	 * 
	 * @param text a string
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	public Tokenizer(String text) {
		Objects.requireNonNull(text);
		this.text = text;
		breakIterator.setText(text);
		start = breakIterator.first();
		end = breakIterator.next();
		primeNext();
		return;
	}

	public boolean hasNext() {
		return next != null;
	}

	private void primeNext() {
		Word word = candidateNext();
		while (word != null && !Character.isLetterOrDigit(word.text().charAt(0))) {
			word = candidateNext();
		}
		next = word;
		return;
	}

	private Word candidateNext() {
		if (end == BreakIterator.DONE) {
			return null;
		} else {
			Word word = Word.of(text.substring(start, end), start);
			start = end;
			end = breakIterator.next();
			return word;
		}
	}

	public Word next() {
		if (!queue.isEmpty()) {
			return queue.removeFirst();
		}
		Word word = next;
		primeNext();
		return handler.handle(word, text, queue);
	}
}
