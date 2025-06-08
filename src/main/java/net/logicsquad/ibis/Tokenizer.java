package net.logicsquad.ibis;

import java.text.BreakIterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class Tokenizer {
	private final BreakIterator breakIterator = BreakIterator.getWordInstance();

	private final String text;

	private int start;

	private int end;

	private Queue<Word> queue = new LinkedList<>(); 

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
		return;
	}

	public boolean hasNext() {
		return end != BreakIterator.DONE;
	}

	private Word candidateNext() {
		Word word = Word.of(text.substring(start, end), start);
		start = end;
		end = breakIterator.next();
		return word;
	}

	public Word next() {
		if (!queue.isEmpty()) {
			return queue.remove();
		}
		Word word = candidateNext();
		while (!Character.isLetterOrDigit(word.text().charAt(0))) {
			word = candidateNext();
		}
		if (word.text().contains("—")) {
			int i = word.text().indexOf('—');
			queue.add(Word.of(word.text().substring(i + 1), word.start() + i + 1));
			return Word.of(word.text().substring(0, i), word.start());
		} else if ("e.g".equals(word.text())) {
			if ('.' == text.charAt(end - 1)) {
				return Word.of("e.g.", word.start());
			} else {
				return word;
			}
		} else if ("i.e".equals(word.text())) {
			if ('.' == text.charAt(end - 1)) {
				return Word.of("i.e.", word.start());
			} else {
				return word;
			}
		} else {
			return word;
		}
	}
}
